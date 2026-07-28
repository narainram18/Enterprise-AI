package test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Test {
    public static void main(String[] args) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/enterprise_ai", "postgres", "postgres");
        Statement s = c.createStatement();
        s.execute("UPDATE knowledge_documents SET processing_status = 'READY' WHERE id = 13");
        c.close();
    }
}
