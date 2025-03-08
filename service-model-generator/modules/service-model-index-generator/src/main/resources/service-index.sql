-- Drop tables if they already exist to prevent conflicts
DROP TABLE IF EXISTS Package;
DROP TABLE IF EXISTS Listener;
DROP TABLE IF EXISTS Parameter;
DROP TABLE IF EXISTS ParameterMemberType;
DROP TABLE IF EXISTS Annotation;
DROP TABLE IF EXISTS AnnotationField;
DROP TABLE IF EXISTS AnnotationFieldMemberType;

-- Create Package table
CREATE TABLE Package (
    package_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    org TEXT NOT NULL,
    version TEXT,
    keywords TEXT
);

-- Create Function table
CREATE TABLE Listener (
    listener_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    return_error INTEGER CHECK(return_error IN (0, 1)),
    package_id INTEGER,
    FOREIGN KEY (package_id) REFERENCES Package(package_id) ON DELETE CASCADE
);

-- Create Parameter table
CREATE TABLE Parameter (
    parameter_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    kind TEXT CHECK(kind IN ('REQUIRED', 'DEFAULTABLE', 'INCLUDED_RECORD', 'REST_PARAMETER',
    'INCLUDED_FIELD', 'INCLUDED_RECORD_REST', 'PARAM_FOR_TYPE_INFER', 'PATH_PARAM', 'PATH_REST_PARAM')),
    type JSON, -- JSON type for parameter type information
    default_value TEXT,
    optional INTEGER CHECK(optional IN (0, 1)),
    import_statements TEXT,
    listener_id INTEGER,
    FOREIGN KEY (listener_id) REFERENCES Listener(listener_id) ON DELETE CASCADE
);

-- Create Parameter Member Type table
CREATE TABLE ParameterMemberType (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    type JSON, -- JSON type for parameter type information
    kind TEXT,
    parameter_id INTEGER,
    package TEXT, -- format of the package is org:name:version
    FOREIGN KEY (parameter_id) REFERENCES Parameter(parameter_id) ON DELETE CASCADE
);

-- Create Annotation table
CREATE TABLE Annotation (
    annotation_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    attachment_points TEXT NOT NULL,
    package_id INTEGER,
    FOREIGN KEY (package_id) REFERENCES Package(package_id) ON DELETE CASCADE
);

-- Create Annotation Attachment table
CREATE TABLE AnnotationField (
    annotation_field_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    kind TEXT CHECK(kind IN ('REQUIRED', 'DEFAULTABLE', 'INCLUDED_RECORD','INCLUDED_FIELD', 'INCLUDED_RECORD_REST')),
    type JSON, -- JSON type for parameter type information
    default_value TEXT,
    optional INTEGER CHECK(optional IN (0, 1)),
    import_statements TEXT,
    annotation_id INTEGER,
    FOREIGN KEY (annotation_id) REFERENCES Annotation(annotation_id) ON DELETE CASCADE
);

-- Create Annotation Field Member Type table
CREATE TABLE AnnotationFieldMemberType (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    type JSON, -- JSON type for parameter type information
    kind TEXT,
    annotation_field_id INTEGER,
    package TEXT, -- format of the package is org:name:version
    FOREIGN KEY (annotation_field_id) REFERENCES AnnotationField(annotation_field_id) ON DELETE CASCADE
);
