package com.joysistvi.CyberAccess.repo;
import com.joysistvi.CyberAccess.model.Conversation; import java.sql.SQLException; import java.util.List;
public interface ConversationRepo { List<Conversation> findAll()throws SQLException; List<Conversation> findByUser(int userId)throws SQLException; boolean save(Conversation x)throws SQLException; boolean updateStatus(int id,String status)throws SQLException; boolean delete(int id)throws SQLException; }
