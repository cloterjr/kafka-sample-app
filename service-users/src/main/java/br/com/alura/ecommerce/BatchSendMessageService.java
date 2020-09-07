package br.com.alura.ecommerce;

import br.com.alura.ecommerce.consumer.ConsumerService;
import br.com.alura.ecommerce.consumer.KafkaService;
import br.com.alura.ecommerce.consumer.ServiceRunner;
import br.com.alura.ecommerce.dispatcher.KafkaDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BatchSendMessageService implements ConsumerService<String> {
    private final Connection connection;

    BatchSendMessageService() throws SQLException {
        String url = "jdbc:sqlite:target/users_database.db";
        connection = DriverManager.getConnection(url);

        try{
            connection.createStatement().execute("create table Users(" +
                    "uuid varchar(200) primary key," +
                    "email varchar(200))");
        }catch(SQLException ex) {
            //be careful, the sql could be wrong, be reallly careful
            ex.printStackTrace();
        }
    }

    public static void main(String[] args){
        new ServiceRunner(BatchSendMessageService::new).start(1);
    }

    @Override
    public String getTopic() {
        return "ECOMMERCE_SEND_MESSAGE_TO_ALL_USERS";
    }

    @Override
    public String getConsumerGroup() {
        return BatchSendMessageService.class.getSimpleName();
    }

    private final KafkaDispatcher<User> userDispatcher = new KafkaDispatcher<>();

    public void parse(ConsumerRecord<String, Message<String>> record) throws SQLException{
        System.out.println("----------------------------------------");
        System.out.println("Processing new batch");
        System.out.println("Topic:" + record.value());

        var message = record.value();
        for(User user : getAllUsers()){
            userDispatcher.sendAsync(message.getPayload(), user.getUuid(), message.getId().continueWith(BatchSendMessageService.class.getSimpleName()),user);
            System.out.println("Acho que enviei para " + user);
        }
    }

    private List<User> getAllUsers() throws SQLException {
        var results = connection.prepareStatement("select uuid from users").executeQuery();

        List<User> users = new ArrayList<>();

        while(results.next()){
            users.add(new User(results.getString(1)));
        }

        return users;
    }
}
