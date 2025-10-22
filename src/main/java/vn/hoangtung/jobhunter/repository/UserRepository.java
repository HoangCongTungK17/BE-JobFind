package vn.hoangtung.jobhunter.repository;

import org.springframework.stereotype.Repository;

import vn.hoangtung.jobhunter.domain.Company;
import vn.hoangtung.jobhunter.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Spring Data JPA tự động tạo các phương thức CRUD cơ bản.
    // Chúng ta có thể thêm các phương thức truy vấn tùy chỉnh nếu cần.
    User findByEmail(String email);

    boolean existsByEmail(String email);

    User findByRefreshTokenAndEmail(String token, String email);

    // List<User> findByCompany(Company company);

}
