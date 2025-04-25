package edu.lcaitlyn.cloudfilestorage.repository;

import edu.lcaitlyn.cloudfilestorage.models.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {
    @Override
    public void save(User user) {

    }

    @Override
    public User findById(long id) {
        return null;
    }

    @Override
    public User findByUsername(String username) {
        return null;
    }

    @Override
    public List<User> findAll() {
        return List.of(
                new User("qwe", "qwe"),
                new User("zxc", "zxc"),
                new User("123", "123")
        );
    }
}
