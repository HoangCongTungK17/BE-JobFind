package vn.hoangtung.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.hoangtung.jobhunter.domain.User;
import vn.hoangtung.jobhunter.domain.dto.Meta;
import vn.hoangtung.jobhunter.domain.dto.ResCreateUserDTO;
import vn.hoangtung.jobhunter.domain.dto.ResUpdateUserDTO;
import vn.hoangtung.jobhunter.domain.dto.ResUserDTO;
import vn.hoangtung.jobhunter.domain.dto.ResultPaginationDTO;
import vn.hoangtung.jobhunter.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Constructor injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tạo một người dùng mới.
     */
    public User handleCreateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Lấy thông tin người dùng theo ID.
     */
    public User getUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);
    }

    /**
     * Lấy danh sách tất cả người dùng.
     */
    public ResultPaginationDTO fetchAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        Meta mt = new Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        // List<ResUserDTO> listUser = pageUser.getContent()
        // .stream().map(item -> this.convertToResUserDTO(
        // item))
        // .collect(Collectors.toList());

        // rs.setResult(listUser);

        return rs;
    }

    /**
     * Cập nhật thông tin người dùng.
     */
    public User handleUpdateUser(Long userId, User userDetails) {
        return userRepository.findById(userId).map(user -> {
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            user.setPassword(userDetails.getPassword());
            return userRepository.save(user);
        }).orElse(null);
    }

    /**
     * Xóa một người dùng theo ID.
     */
    public void handleDeleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public boolean isEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO res = new ResCreateUserDTO();
        ResCreateUserDTO.CompanyUser com = new ResCreateUserDTO.CompanyUser();

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());

        // if (user.getCompany() != null) {
        // com.setId(user.getCompany().getId());
        // com.setName(user.getCompany().getName());
        // res.setCompany(com);
        // }
        return res;
    }

    public User fetchUserById(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetchUserById'");
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO res = new ResUpdateUserDTO();
        ResUpdateUserDTO.CompanyUser com = new ResUpdateUserDTO.CompanyUser();
        // if (user.getCompany() != null) {
        // com.setId(user.getCompany().getId());
        // com.setName(user.getCompany().getName());
        // res.setCompany(com);
        // }
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        return res;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO res = new ResUserDTO();
        // ResUserDTO.CompanyUser com = new ResUserDTO.CompanyUser();

        // ResUserDTO.RoleUser roleUser = new ResUserDTO.RoleUser();
        // if (user.getCompany() != null) {
        // com.setId(user.getCompany().getId());
        // com.setName(user.getCompany().getName());
        // res.setCompany(com);
        // }

        // if (user.getRole() != null) {
        // roleUser.setId(user.getRole().getId());
        // roleUser.setName(user.getRole().getName());
        // res.setRole(roleUser);
        // }

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        return res;
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }
}
