package cn.zjnktion.richer.connector.db.dao;

import cn.zjnktion.richer.connector.JDBC;
import cn.zjnktion.richer.connector.db.dto.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author zjnktion
 */
public class UserDao {

    private static final UserDao INSTANCE = new UserDao();

    private UserDao() {

    }

    public static UserDao getInstance() {
        return INSTANCE;
    }

    public User findUserByAccount(String account) {
        StringBuilder sql = new StringBuilder("select * from t_user where account=?");

        try {
            return JDBC.getTemplate().queryForObject(sql.toString(), new RowMapper<User>() {
                public User mapRow(ResultSet resultSet, int i) throws SQLException {
                    User user = new User();
                    user.setId(resultSet.getInt("id"));
                    user.setAccount(resultSet.getString("account"));
                    user.setPwd(resultSet.getString("pwd"));
                    user.setLoginToken(resultSet.getString("login_token"));
                    return user;
                }
            }, account);
        }
        catch (Exception e) {
            return null;
        }
    }

    public User findUserByLoginToken(String loginToken) {
        StringBuilder sql = new StringBuilder("select * from t_user where login_toke=?");

        try {
            return JDBC.getTemplate().queryForObject(sql.toString(), new RowMapper<User>() {
                public User mapRow(ResultSet resultSet, int i) throws SQLException {
                    User user = new User();
                    user.setId(resultSet.getInt("id"));
                    user.setAccount(resultSet.getString("account"));
                    user.setPwd(resultSet.getString("pwd"));
                    user.setLoginToken(resultSet.getString("login_token"));
                    return user;
                }
            }, loginToken);
        }
        catch (Exception e) {
            return null;
        }
    }

    public int updateLoginToken(int id, String loginToken) {
        StringBuilder sql = new StringBuilder("update t_user set login_token=? where id=?");

        return JDBC.getTemplate().update(sql.toString(), loginToken, id);
    }
}
