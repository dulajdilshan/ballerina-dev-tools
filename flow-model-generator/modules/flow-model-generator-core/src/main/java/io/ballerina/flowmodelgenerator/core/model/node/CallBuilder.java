/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;

/**
 * Abstract base class for function-like builders (functions, methods, resource actions).
 *
 * @since 2.0.0
 */
public abstract class CallBuilder extends NodeBuilder {

    protected abstract NodeKind getFunctionNodeKind();

    protected abstract FunctionData.Kind getFunctionResultKind();

    @Override
    public void setConcreteConstData() {
        codedata().node(getFunctionNodeKind());
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();

        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(codedata.symbol())
                .moduleInfo(new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                .functionResultKind(getFunctionResultKind())
                .userModuleInfo(moduleInfo);

        if (getFunctionNodeKind() != NodeKind.FUNCTION_CALL) {
            functionDataBuilder.parentSymbolType(codedata.object());
        }

        // Set the semantic model if the function is local
        boolean isLocalFunction = isLocalFunction(context.workspaceManager(), context.filePath(), codedata);
        if (isLocalFunction) {
            WorkspaceManager workspaceManager = context.workspaceManager();
            PackageUtil.loadProject(context.workspaceManager(), context.filePath());
            SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            functionDataBuilder.semanticModel(semanticModel);
        }
        FunctionData functionData = functionDataBuilder.build();

        metadata()
                .label(functionData.name())
                .icon(CommonUtils.generateIcon(functionData.org(), functionData.packageName(),
                        functionData.version()))
                .description(functionData.description());
        codedata()
                .id(functionData.functionId())
                .node(getFunctionNodeKind())
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .symbol(codedata.symbol());

        if (getFunctionNodeKind() != NodeKind.FUNCTION_CALL) {
            properties().custom()
                    .metadata()
                        .label(Property.CONNECTION_LABEL)
                        .description(Property.CONNECTION_DOC)
                        .stepOut()
                    .typeConstraint(isLocalFunction ? codedata.object() :
                            CommonUtils.getClassType(codedata.module(), codedata.object()))
                    .value(codedata.parentSymbol())
                    .type(Property.ValueType.IDENTIFIER)
                    .stepOut()
                    .addProperty(Property.CONNECTION_KEY);
        }
        setParameterProperties(functionData);

        String returnTypeName = functionData.returnType();
        if (CommonUtils.hasReturn(functionData.returnType())) {
            properties()
                    .type(returnTypeName, functionData.inferredReturnType())
                    .data(returnTypeName, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
        }

        if (functionData.returnError()) {
            properties().checkError(true);
        }
    }

    protected void setParameterProperties(FunctionData function) {
        boolean hasOnlyRestParams = function.parameters().size() == 1;
        for (ParameterData paramResult : function.parameters().values()) {
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional());

            switch (paramResult.kind()) {
                case INCLUDED_RECORD_REST -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    unescapedParamName = "additionalValues";
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                }
                case REST_PARAMETER -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                }
                default -> customPropBuilder.type(Property.ValueType.EXPRESSION);
            }

            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    protected void setReturnTypeProperties(String returnTypeName, TemplateContext context, boolean editable,
                                           String label) {
        properties()
                .type(returnTypeName, editable)
                .data(returnTypeName, context.getAllVisibleSymbolNames(), label);
    }

    protected void setExpressionProperty(Codedata codedata) {
        properties().custom()
                .metadata()
                    .label(Property.CONNECTION_LABEL)
                    .description(Property.CONNECTION_DOC)
                    .stepOut()
                .typeConstraint(CommonUtils.getClassType(codedata.module(), codedata.object()))
                .value(codedata.parentSymbol())
                .type(Property.ValueType.IDENTIFIER)
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);
    }

    protected static boolean isLocalFunction(WorkspaceManager workspaceManager, Path filePath, Codedata codedata) {
        if (codedata.org() == null || codedata.module() == null || codedata.version() == null) {
            return true;
        }
        try {
            Project project = workspaceManager.loadProject(filePath);
            PackageDescriptor descriptor = project.currentPackage().descriptor();
            String packageOrg = descriptor.org().value();
            String packageName = descriptor.name().value();
            String packageVersion = descriptor.version().value().toString();

            return packageOrg.equals(codedata.org())
                    && packageName.equals(codedata.module())
                    && packageVersion.equals(codedata.version());
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return false;
        }
    }
}
