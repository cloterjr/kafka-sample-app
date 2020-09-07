package br.com.alura.ecommerce;

import br.com.alura.ecommerce.consumer.ConsumerService;
import br.com.alura.ecommerce.consumer.KafkaService;
import br.com.alura.ecommerce.consumer.ServiceRunner;
import br.com.alura.ecommerce.dispatcher.KafkaDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class EmailNewOrderService implements ConsumerService<Order> {
    public static void main(String[] args) {
        new ServiceRunner(EmailNewOrderService::new).start(1);
    }

    @Override
    public String getTopic() {
        return "ECOMMERCE_NEW_ORDER";
    }

    @Override
    public String getConsumerGroup() {
        return "1" + EmailNewOrderService.class.getSimpleName();
    }

    private final KafkaDispatcher<Email> emailDispatcher = new KafkaDispatcher<>();

    public void parse(ConsumerRecord<String, Message<Order>> record) throws ExecutionException, InterruptedException {
        System.out.println("----------------------------------------");
        System.out.println("Processing new order, preparing email");
        System.out.println(record.key());
        final Message<Order> message = record.value();
        final Order order = message.getPayload();
        System.out.println();
        System.out.println(record.partition());
        System.out.println(record.offset());

        final String email = order.getEmail();
        final CorrelationId correlationId = message.getId().continueWith(EmailNewOrderService.class.getSimpleName());

        var emailCode = new Email("New Order Mail", "Thank you for your order! We are processing your order!");
        emailDispatcher.send("ECOMMERCE_SEND_EMAIL", email, correlationId, emailCode);
    }

    private boolean isFraud(Order order) {
        return order.getAmount().compareTo(new BigDecimal("4500")) >= 0;
    }
}
