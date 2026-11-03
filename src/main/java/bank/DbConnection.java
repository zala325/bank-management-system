package bank;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/** Connects to MongoDB. The database "bank_db" is created automatically. */
public class DbConnection {

    private static final String URI =
            "mongodb://localhost:27017/?serverSelectionTimeoutMS=5000";
    private static final String DB_NAME = "bank_db";

    private static MongoClient client;

    public static MongoDatabase getDatabase() {
        if (client == null) {
            client = MongoClients.create(URI);
        }
        return client.getDatabase(DB_NAME);
    }

    public static void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
