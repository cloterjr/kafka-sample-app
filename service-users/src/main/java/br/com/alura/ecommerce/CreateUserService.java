package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CreateUserService {

    private final Connection connection;

    CreateUserService() throws SQLException {
        String url = "jdbc:sqlite:service-users/target/users_database.db";
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

    public static void main(String[] args) throws SQLException, ExecutionException, InterruptedException {
        var createUserService = new CreateUserService();
        try(var service = new KafkaService(
                CreateUserService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                createUserService::parse,
                Map.of())){
            service.run();
        };
    }

    void parse(ConsumerRecord<String, Message<Order>> record) throws SQLException {
        System.out.println("----------------------------------------");
        System.out.println("Processing new order, checking for new user");
        System.out.println(record.value());
        var message = record.value();
        final Order order = message.getPayload();

        if(isNewUser(order.getEmail())) {
            insertNewUser(order.getEmail());
        }



    }

    private void insertNewUser(String email) throws SQLException {
        var insert = connection.prepareStatement("insert into Users(uuid, email) values(?,?)");
        final String uuid = UUID.randomUUID().toString();
        insert.setString(1, uuid);
        insert.setString(2, email);
        insert.execute();
        System.out.println("User " + uuid + " and " + email + " added");
    }

    private boolean isNewUser(String email) throws SQLException {
        final PreparedStatement exists = connection.prepareStatement("select uuid from Users where email = ? limit 1");
        exists.setString(1, email);
        final ResultSet results = exists.executeQuery();

        return !results.next();
    }

}
