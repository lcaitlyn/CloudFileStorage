package edu.lcaitlyn.cloudfilestorage.service;

import edu.lcaitlyn.cloudfilestorage.models.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    public void save(User user);
    public User findById(long id);
    public User findByUsername(String username);
    public List<User> findAll();

}
