package com.guyporat.database;

import com.guyporat.MainServer;
import com.guyporat.database.model.DeviceModel;
import com.guyporat.database.model.TenantModel;
import com.guyporat.networking.client.DeviceClient;
import com.guyporat.networking.client.states.DeviceSettings;
import com.guyporat.utils.Logger;
import com.guyporat.utils.gson.GsonUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {

    private static Connection connection;

    public void initialize() throws SQLException {
        // Initialize database connection
        String databaseURL = "jdbc:sqlite:" + MainServer.getConfig().get("database_file");
        System.out.println("Database URL: " + databaseURL);

        connection = DriverManager.getConnection(databaseURL);
        if (connection != null) {
            DatabaseMetaData meta = connection.getMetaData();
            Logger.debug("The driver name is " + meta.getDriverName());
        } else {
            Logger.error("Failed to connect to database");
            return;
        }

        Logger.debug("Loading database schema...");
        // Load database schema
        try (Statement statement = connection.createStatement()) {
            String tenantTableCreationQuery = "CREATE TABLE IF NOT EXISTS `tenants` ("
                    + "`uuid` TEXT(36) NOT NULL,"
                    + "`full_name` TEXT NOT NULL,"
                    + "`webUser` INT(1) NOT NULL,"
                    + "`username` TEXT,"
                    + "`hashedPassword` TEXT,"
                    + "`faceDataType` TEXT NOT NULL,"
                    + "`faceDataPath` TEXT NOT NULL,"
                    + "PRIMARY KEY (`uuid`)"
                    + ");";
            statement.execute(tenantTableCreationQuery);

            String deviceTableCreationQuery = "CREATE TABLE IF NOT EXISTS `devices` (" +
                    "`uuid` TEXT(36) NOT NULL," +
                    "`device_type` TEXT NOT NULL," +
                    "`settings` TEXT NOT NULL," +
                    "`secret` TEXT NOT NULL," +
                    "PRIMARY KEY (`uuid`)" +
                    ");";
            statement.execute(deviceTableCreationQuery);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }


    public static class DeviceTableManager {
        private static final DeviceTableManager instance = new DeviceTableManager();

        public static DeviceTableManager getInstance() {
            return instance;
        }

        public DeviceModel getDeviceByUUID(UUID uuid) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM devices WHERE uuid = ?");
                DeviceModel resultSet = getDeviceByParam(uuid.toString(), statement);
                if (resultSet != null) return resultSet;
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
            return null;
        }

        public void addDevice(DeviceModel device) {
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO devices (uuid, device_type, settings, secret) VALUES (?, ?, ?, ?)");
                statement.setString(1, device.deviceUUID.toString());
                statement.setString(2, device.deviceType.name());
                statement.setString(3, GsonUtils.getGson().toJson(device.deviceSettings, DeviceSettings.class));
                statement.setString(4, device.secret);
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error("Error adding device: " + e.getMessage());
            }
        }

        public void updateDevice(DeviceModel device) {
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE devices SET device_type = ?, settings = ?, secret = ? WHERE uuid = ?");
                statement.setString(1, device.deviceType.name());
                statement.setString(2, GsonUtils.getGson().toJson(device.deviceSettings, DeviceSettings.class));
                statement.setString(3, device.secret);
                statement.setString(4, device.deviceUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error("Error updating device: " + e.getMessage());
            }
        }

        public void removeDevice(UUID uuid) {
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM devices WHERE uuid = ?");
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
        }

        public List<DeviceModel> getDevices() {
            List<DeviceModel> devices = new ArrayList<>();
            String query = "SELECT * FROM devices";

            return getDevicesByParam(devices, query);
        }

        public List<DeviceModel> getCameraDevices() {
            List<DeviceModel> devices = new ArrayList<>();
            String query = "SELECT * FROM devices WHERE device_type = 'CAMERA'";

            return getDevicesByParam(devices, query);
        }

        private List<DeviceModel> getDevicesByParam(List<DeviceModel> returnList, String query) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    DeviceModel device = constructDeviceModel(
                            UUID.fromString(resultSet.getString("uuid")),
                            DeviceClient.IOTDeviceType.valueOf(resultSet.getString("device_type")),
                            resultSet.getString("settings"),
                            resultSet.getString("secret")
                    );
                    returnList.add(device);
                }
            } catch (SQLException e) {
                Logger.error("Error retrieving devices: " + e.getMessage());
            }
            return returnList;
        }

        private DeviceModel getDeviceByParam(String param, PreparedStatement statement) throws SQLException {
            statement.setString(1, param);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return constructDeviceModel(
                            UUID.fromString(resultSet.getString("uuid")),
                            DeviceClient.IOTDeviceType.valueOf(resultSet.getString("device_type")),
                            resultSet.getString("settings"),
                            resultSet.getString("secret")
                    );
                }
            }
            return null;
        }

        private DeviceModel constructDeviceModel(UUID uuid, DeviceClient.IOTDeviceType deviceType, String settings, String secret) {
            return new DeviceModel(uuid, deviceType, GsonUtils.getGson().fromJson(settings, DeviceSettings.class), secret);
        }
    }


    public static class TenantTableManager {

        private static final TenantTableManager instance = new TenantTableManager();

        public static TenantTableManager getInstance() {
            return instance;
        }

        /**
         * Search for a tenant by UUID in the database
         *
         * @param uuid UUID of the tenant
         * @return TenantModel object if found, null otherwise
         */
        public TenantModel getTenantByUUID(UUID uuid) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM tenants WHERE uuid = ?");
                TenantModel resultSet = getTenantByParam(uuid.toString(), statement);
                if (resultSet != null) return resultSet;
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
            return null;
        }

        /**
         * Retrieve a list of all tenants from the database.
         *
         * @return List of TenantModel objects
         */
        public List<TenantModel> getTenants() {
            List<TenantModel> tenants = new ArrayList<>();
            String query = "SELECT * FROM tenants";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    TenantModel tenant = new TenantModel(
                            resultSet.getString("full_name"),
                            resultSet.getBoolean("webUser"),
                            resultSet.getString("username"),
                            resultSet.getString("hashedPassword"),
                            resultSet.getString("faceDataType"),
                            resultSet.getString("faceDataPath"),
                            UUID.fromString(resultSet.getString("uuid"))
                    );
                    tenants.add(tenant);
                }
            } catch (SQLException e) {
                Logger.error("Error retrieving tenants: " + e.getMessage());
            }
            return tenants;
        }

        public void addTenant(TenantModel tenant) {
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO tenants (uuid, full_name, webUser, username, hashedPassword, faceDataType, faceDataPath) VALUES (?, ?, ?, ?, ?, ?, ?)");
                statement.setString(1, tenant.getUUID().toString());
                statement.setString(2, tenant.getFullName());
                statement.setBoolean(3, tenant.isWebUser());
                statement.setString(4, tenant.getUsername());
                statement.setString(5, tenant.getHashedPassword());
                statement.setString(6, tenant.getFaceDataType());
                statement.setString(7, tenant.getFaceDataPath());
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error("Error adding tenant: " + e.getMessage());
            }
        }

        public TenantModel getTenantByUsername(String username) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM tenants WHERE username = ?");
                TenantModel resultSet = getTenantByParam(username, statement);
                if (resultSet != null) return resultSet;
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
            return null;
        }

        public boolean doesTenantExistUsername(String username) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM tenants WHERE username = ?");
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
            return false;
        }

        public boolean doesTenantExistUUID(UUID uuid) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM tenants WHERE uuid = ?");
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
            return false;
        }

        public void removeTenant(UUID uuid) {
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM tenants WHERE uuid = ?");
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
        }

        public void updateTenant(TenantModel tenant) {
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE tenants SET full_name = ?, webUser = ?, username = ?, hashedPassword = ?, faceDataType = ?, faceDataPath = ? WHERE uuid = ?");
                statement.setString(1, tenant.getFullName());
                statement.setBoolean(2, tenant.isWebUser());
                statement.setString(3, tenant.getUsername());
                statement.setString(4, tenant.getHashedPassword());
                statement.setString(5, tenant.getFaceDataType());
                statement.setString(6, tenant.getFaceDataPath());
                statement.setString(7, tenant.getUUID().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Logger.error(e.getMessage());
            }
        }

        private TenantModel getTenantByParam(String username, PreparedStatement statement) throws SQLException {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new TenantModel(
                            resultSet.getString("full_name"),
                            resultSet.getBoolean("webUser"),
                            resultSet.getString("username"),
                            resultSet.getString("hashedPassword"),
                            resultSet.getString("faceDataType"),
                            resultSet.getString("faceDataPath"),
                            UUID.fromString(resultSet.getString("uuid"))
                    );
                }
            }
            return null;
        }

    }
}
