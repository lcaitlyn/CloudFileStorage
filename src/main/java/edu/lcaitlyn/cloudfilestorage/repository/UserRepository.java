package edu.lcaitlyn.cloudfilestorage.repository;

import edu.lcaitlyn.cloudfilestorage.models.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository {
    public void save(User user);

    public User findById(long id);

    public User findByUsername(String username);

    public List<User> findAll();
}
