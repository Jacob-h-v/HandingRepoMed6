package com.example.med6grp5supercykelstig;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class DBQueryFacilitator extends AsyncTask<String, Void, DBQueryFacilitator.QueryResult> {
    private final Callback callback;
    private final boolean executeUpdate;

    public DBQueryFacilitator(Callback callback, boolean executeUpdate) {
        this.callback = callback;
        this.executeUpdate = executeUpdate;
    }

    @Override
    protected QueryResult doInBackground(String... args) {
        String host, port, databaseName, userName, password, query;
        host = port = databaseName = userName = password = query = null;
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "-host":
                    host = args[++i];
                    break;
                case "-username":
                    userName = args[++i];
                    break;
                case "-password":
                    password = args[++i];
                    break;
                case "-database":
                    databaseName = args[++i];
                    break;
                case "-port":
                    port = args[++i];
                    break;
                case "-query":
                    query = args[++i];
                    break;
            }
        }
        // JDBC allows nullable username and password
        if (host == null || port == null || databaseName == null || query == null) {
            return new QueryResult(false, null); // Indicate failure

        }

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require", userName, password);
            statement = connection.createStatement();

            if (executeUpdate) {
                int rowsAffected = statement.executeUpdate(query);
                return new QueryResult(rowsAffected > 0, null); // Return true if rows were affected (insertion successful), false otherwise
            } else {
                Log.d("Executing executeQuery()...", "Executing executeQuery() with following query: "+query);
                resultSet = statement.executeQuery(query);
                StringBuilder response = new StringBuilder();
                while (resultSet.next()){
                    response.append(resultSet.getString(1)).append("\n");
                }
                boolean result = !response.toString().isEmpty();
                String resultValue = "";
                resultValue = response.toString();
                Log.d("Response from DB", "Response from DB: "+response);
                return new QueryResult(result, resultValue); // Return both the success status and the result value
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return new QueryResult(false, null); // Indicate failure

        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostExecute(QueryResult result) {
        // Pass the result to the callback
        callback.onQueryComplete(result.isSuccess(), result.getResultValue());
    }


    // Define a callback interface
    public interface Callback {
        void onQueryComplete(boolean success, String resultValue);
    }

    // Defining a custom class to hold the success state and response from database.
    public static class QueryResult {
        private final boolean success;
        private final String resultValue;

        public QueryResult(boolean success, String resultValue) {
            this.success = success;
            this.resultValue = resultValue;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResultValue() {
            return resultValue;
        }
    }


}
