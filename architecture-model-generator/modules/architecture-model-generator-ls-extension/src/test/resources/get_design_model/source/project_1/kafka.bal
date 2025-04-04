import ballerina/log;
import ballerinax/kafka;

type Order readonly & record {
    int orderId;
    string productName;
    decimal price;
    boolean isValid;
};

listener kafka:Listener orderListener = new (kafka:DEFAULT_URL, {
    groupId: "order-group-id",
    topics: orderTopics
});

service on orderListener {

    remote function onConsumerRecord(Order[] orders) {
        // The set of orders received by the service are processed one by one.
        from Order 'order in orders
        where 'order.isValid
        do {
            log:printInfo(string `Received valid order for ${'order.productName}`);
        };
    }
}

service on new kafka:Listener(kafka:DEFAULT_URL, {
    groupId: "order-group-id",
    topics: "order-topic"
}) {

    remote function onConsumerRecord(Order[] orders) {
        // The set of orders received by the service are processed one by one.
        from Order 'order in orders
        where 'order.isValid
        do {
            log:printInfo(string `Received valid order for ${'order.productName}`);
        };
    }
}
