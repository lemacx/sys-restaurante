package org.sysRestaurante.model;

import org.sysRestaurante.applet.AppFactory;
import org.sysRestaurante.etc.Employee;
import org.sysRestaurante.etc.Manager;
import org.sysRestaurante.etc.User;
import org.sysRestaurante.util.DBConnection;
import org.sysRestaurante.util.Encryption;
import org.sysRestaurante.util.ExceptionHandler;
import org.sysRestaurante.util.LoggerHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Authentication {

    private static final Logger LOGGER = LoggerHandler.getGenericConsoleHandler(Authentication.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static Connection con;

    public Authentication() {
        try {
            con = DBConnection.getConnection();
            if (con != null)
                LOGGER.info("Successful connection to the database.");

        } catch (SQLException ex) {
            ExceptionHandler.incrementGlobalExceptionsCount();
            LOGGER.severe("Connection to database couldn't be established.");
            ex.printStackTrace();
        }
    }

    public boolean isDatabaseConnected() {
        return con != null;
    }

    public int loginRequested(String user, String pass) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String query = "SELECT * FROM usuario WHERE username = ? and senha = ?";
        String password = Encryption.encrypt(pass);

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, user);
            ps.setString(2, password);

            rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("IdUsuario");
                AppFactory appFactory = new AppFactory();
                if (!rs.getBoolean("isAdmin")) {
                    appFactory.setUser(new Employee(
                            rs.getString("nome"),
                            rs.getString("senha"),
                            rs.getString("username"),
                            rs.getString("email")
                    ));
                    updateSessionTable(userId);
                    return 0;
                }
                else if (rs.getBoolean("isAdmin")) {
                    updateSessionTable(userId);
                    appFactory.setUser(new Manager(
                            rs.getString("nome"),
                            rs.getString("senha"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getBoolean("isAdmin")
                    ));
                    return 1;
                }
            } else return 2;

        } finally {
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        }
        return 1;
    }

    public User getUserData(String username) {
        PreparedStatement ps;
        ResultSet rs;
        String query = "SELECT * FROM usuario WHERE username = ?";

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, username);
            rs = ps.executeQuery();
            User user = new User();
            while (rs.next()) {
                user.setIdUsuario(rs.getInt("idUsuario"));
                user.setName(rs.getString("nome"));
                user.setUsername(username);
                user.setEmail(rs.getString("email"));
                user.setAdmin(rs.getBoolean("isAdmin"));
            }

            ps.close();
            rs.close();

            return user;
        } catch (SQLException e) {
            LOGGER.severe("Couldn't get user data.");
            ExceptionHandler.incrementGlobalExceptionsCount();
            e.printStackTrace();
        }
        return null;
    }

    public void updateSessionTable(int userId) {
        LocalDateTime date = LocalDateTime.now();
        PreparedStatement ps;
        String query = "INSERT INTO sessao (idUsuario, dataSessao, tempoSessao) VALUES (?, ?, ?)";

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(date.toLocalDate()));
            ps.setTime(3, java.sql.Time.valueOf(date.toLocalTime()));

            ps.executeUpdate();
            ps.close();

            LOGGER.setLevel(Level.ALL);
            LOGGER.config("Session time: " + DATE_FORMAT.format(date));
        } catch (SQLException ex) {
            LOGGER.severe("Session couldn't be stored.");
            ExceptionHandler.incrementGlobalExceptionsCount();
            ex.printStackTrace();
        }
    }

    public void setSessionDuration(int userId, int lastSessionID, long sessionTime) {
        PreparedStatement ps;
        String query = "UPDATE sessao SET duracaoSessao = ? WHERE idUsuario = ? and idSessao = ?";

        try  {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            ps.setInt(1, (int) sessionTime);
            ps.setInt(2, userId);
            ps.setInt(3, lastSessionID);
            ps.executeUpdate();

            ps.close();
            LOGGER.info("Last session duration registered.");
        } catch (SQLException e) {
            LOGGER.severe("Couldn't register session duration.");
            ExceptionHandler.incrementGlobalExceptionsCount();
            e.printStackTrace();
        }
    }

    public LocalDateTime getLastSessionDate() {
        PreparedStatement ps;
        ResultSet rs;
        String query = "SELECT * FROM sessao";

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            rs = ps.executeQuery();
            ArrayList<Long> dates = new ArrayList<>();

            while (rs.next()) {
                dates.add(rs.getDate("dataSessao").getTime()
                        + rs.getTime("tempoSessao").getTime()
                        - 10800000L);
            }

            if (dates.isEmpty()) {
                return null;
            } else {
                Long mostRecentSessionLong = Collections.max(dates);
                LocalDateTime mostRecentSession = Instant.ofEpochMilli(mostRecentSessionLong)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                return mostRecentSession;
            }
        } catch (SQLException | NullPointerException ex) {
            LOGGER.severe("Error while getting last session.");
            ExceptionHandler.incrementGlobalExceptionsCount();
            ex.printStackTrace();
        }
        return null;
    }

    public int getLastSessionId() {
        PreparedStatement ps;
        String query = "SELECT idSessao FROM sessao ORDER BY idSessao DESC LIMIT 1";

        try {
            con = DBConnection.getConnection();
            ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            int id = 0;

            if (rs.next()) {
                id = rs.getInt("idSessao");
            }

            ps.close();
            rs.close();

            return id;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
