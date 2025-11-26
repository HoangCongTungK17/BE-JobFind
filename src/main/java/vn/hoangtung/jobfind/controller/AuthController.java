package vn.hoangtung.jobfind.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResCreateUserDTO;
import vn.hoangtung.jobfind.domain.response.ResLoginDTO;
import vn.hoangtung.jobfind.domain.request.ReqLoginDTO;
import vn.hoangtung.jobfind.service.UserService;
import vn.hoangtung.jobfind.util.SecurityUtil;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.util.error.IdInvalidException;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1") // Tiền tố chung cho tất cả các API trong Controller này
public class AuthController {

        // Đây là 'trình quản lý xác thực' của Spring Security.
        // Nó được dùng để kiểm tra thông tin đăng nhập (username/password).
        private final AuthenticationManagerBuilder authenticationManagerBuilder;

        // Lớp tiện ích tùy chỉnh (bạn đã tạo) để tạo và giải mã JWT.
        private final SecurityUtil securityUtil;

        // Service xử lý logic liên quan đến User (lưu/lấy/cập nhật DB).
        private final UserService userService;

        // Service để mã hóa mật khẩu (được định nghĩa Bean trong SecurityConfig).
        private final PasswordEncoder passwordEncoder;

        // Tiêm giá trị thời gian hết hạn của Refresh Token từ application.properties
        @Value("${hoangtung.jwt.refresh-token-validity-in-seconds}")
        private long refreshTokenExpiration;

        // Constructor để Spring tự động tiêm (inject) các Bean cần thiết
        public AuthController(
                        AuthenticationManagerBuilder authenticationManagerBuilder,
                        SecurityUtil securityUtil,
                        UserService userService,
                        PasswordEncoder passwordEncoder) {
                this.authenticationManagerBuilder = authenticationManagerBuilder;
                this.securityUtil = securityUtil;
                this.userService = userService;
                this.passwordEncoder = passwordEncoder;
        }

        /**
         * API Đăng nhập
         * * @param loginDto Đối tượng chứa username và password từ request body.
         * 
         * @return ResponseEntity chứa Access Token trong body và Refresh Token trong
         *         cookie.
         */
        @PostMapping("/auth/login")
        public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDto) {
                // 1. Tạo đối tượng 'Token xác thực' từ username và password người dùng gửi lên.
                // Đây là đối tượng 'chưa được xác thực'.
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                loginDto.getUsername(), loginDto.getPassword());

                // 2. Thực hiện xác thực:
                // Spring Security (thông qua AuthenticationManager) sẽ gọi đến
                // `UserDetailsCustom.loadUserByUsername()` để lấy thông tin user từ DB
                // và dùng `PasswordEncoder` để so sánh mật khẩu.
                // Nếu sai, nó sẽ ném ra AuthenticationException (được xử lý bởi
                // GlobalException).
                Authentication authentication = authenticationManagerBuilder.getObject()
                                .authenticate(authenticationToken);

                // 3. Nếu xác thực thành công, lưu thông tin xác thực vào SecurityContext.
                // Điều này làm cho người dùng được "đăng nhập" trong suốt quá trình xử lý
                // request này.
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 4. Chuẩn bị đối tượng DTO để trả về
                ResLoginDTO res = new ResLoginDTO();
                User currentUserDB = this.userService.handleGetUserByUsername(loginDto.getUsername());

                // Lấy thông tin chi tiết của user để đưa vào response
                if (currentUserDB != null) {
                        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                                        currentUserDB.getId(),
                                        currentUserDB.getEmail(),
                                        currentUserDB.getName(),
                                        currentUserDB.getRole());
                        res.setUser(userLogin);
                }

                // 5. Tạo Access Token (Token truy cập, thời hạn ngắn)
                // authentication.getName() chính là username (email)
                String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
                res.setAccessToken(access_token);

                // 6. Tạo Refresh Token (Token làm mới, thời hạn dài)
                String refresh_token = this.securityUtil.createRefreshToken(loginDto.getUsername(), res);

                // 7. Cập nhật (lưu) Refresh Token vào cơ sở dữ liệu cho user này
                this.userService.updateUserToken(refresh_token, loginDto.getUsername());

                // 8. Tạo cookie để chứa Refresh Token.
                ResponseCookie resCookies = ResponseCookie
                                .from("refresh_token", refresh_token)
                                .httpOnly(true) // CỰC KỲ QUAN TRỌNG: Ngăn JavaScript phía client đọc cookie (chống XSS)
                                .secure(true) // Chỉ gửi cookie qua HTTPS
                                .path("/") // Cookie có hiệu lực trên toàn bộ trang web
                                .maxAge(refreshTokenExpiration) // Đặt thời gian hết hạn cho cookie (vd: 7 ngày)
                                .build();

                // 9. Trả về Response 200 OK
                // - Header `Set-Cookie` chứa Refresh Token
                // - Body chứa Access Token và thông tin User
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                                .body(res);
        }

        /**
         * API Lấy thông tin tài khoản
         * API này yêu cầu Access Token hợp lệ (được Spring Security tự động kiểm tra).
         * * @return Thông tin tài khoản của người dùng đang đăng nhập.
         */
        @GetMapping("/auth/account")
        @ApiMessage("fetch account")
        public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
                // Lấy email của người dùng đã được xác thực từ SecurityContext
                // (Spring Security tự động điền vào đây sau khi giải mã Access Token)
                String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                                : "";

                // Lấy thông tin đầy đủ từ DB
                User currentUserDB = this.userService.handleGetUserByUsername(email);
                ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
                ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();

                // Map thông tin sang DTO
                if (currentUserDB != null) {
                        userLogin.setId(currentUserDB.getId());
                        userLogin.setEmail(currentUserDB.getEmail());
                        userLogin.setName(currentUserDB.getName());
                        // userLogin.setRole(currentUserDB.getRole()); // Bạn có thể thêm Role nếu cần
                        userGetAccount.setUser(userLogin);
                }

                return ResponseEntity.ok().body(userGetAccount);
        }

        /**
         * API Làm mới Token
         * Dùng khi Access Token hết hạn. Client gọi API này với Refresh Token (trong
         * cookie).
         * * @param refresh_token Lấy từ cookie có tên "refresh_token".
         * 
         * @return ResponseEntity chứa Access Token mới và Refresh Token mới.
         * @throws IdInvalidException
         */
        @GetMapping("/auth/refresh")
        @ApiMessage("Get User by refresh token")
        public ResponseEntity<ResLoginDTO> getRefreshToken(
                        // @CookieValue: Tự động trích xuất giá trị từ cookie
                        @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token)
                        throws IdInvalidException {

                // Kiểm tra nếu cookie không tồn tại (giá trị là default)
                if (refresh_token.equals("abc")) {
                        throw new IdInvalidException("Bạn không có refresh token ở cookie");
                }

                // 1. Kiểm tra chữ ký và thời hạn của Refresh Token
                Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
                String email = decodedToken.getSubject(); // Lấy email (subject) từ token

                // 2. Kiểm tra xem Refresh Token này có tồn tại trong DB VÀ khớp với email không
                // Đây là bước bảo mật quan trọng, đảm bảo token chưa bị thu hồi (do logout
                // hoặc đổi mật khẩu)
                User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
                if (currentUser == null) {
                        // Nếu không tìm thấy, có nghĩa là token đã bị vô hiệu hóa
                        throw new IdInvalidException("Refresh Token không hợp lệ");
                }

                // 3. Nếu mọi thứ hợp lệ, cấp lại cặp token MỚI (Token Rotation)
                ResLoginDTO res = new ResLoginDTO();

                // Lấy thông tin user (giống hệt /login)
                User currentUserDB = this.userService.handleGetUserByUsername(email);
                if (currentUserDB != null) {
                        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                                        currentUserDB.getId(),
                                        currentUserDB.getEmail(),
                                        currentUserDB.getName(),
                                        currentUserDB.getRole());
                        res.setUser(userLogin);
                }

                // 4. Tạo Access Token mới
                String access_token = this.securityUtil.createAccessToken(email, res);
                res.setAccessToken(access_token);

                // 5. Tạo Refresh Token mới
                String newRefresh_token = this.securityUtil.createRefreshToken(email, res);

                // 6. Cập nhật Refresh Token MỚI vào DB
                // LƯU Ý: Dòng code gốc `this.userService.updateUserToken(refresh_token,
                // email);`
                // có vẻ là một LỖI.
                // Nó đang lưu lại token CŨ. Đúng ra phải là lưu token MỚI.
                // Dòng đúng phải là:
                this.userService.updateUserToken(newRefresh_token, email);
                // (Nếu bạn vẫn dùng `this.userService.updateUserToken(refresh_token, email);`
                // thì
                // token cũ vẫn hợp lệ,
                // không đạt được mục đích "rotation" - xoay vòng token)

                // 7. Tạo cookie MỚI cho Refresh Token MỚI
                ResponseCookie resCookies = ResponseCookie
                                .from("refresh_token", newRefresh_token)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(refreshTokenExpiration)
                                .build();

                // 8. Trả về response chứa token mới và cookie mới
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                                .body(res);
        }

        /**
         * API Đăng xuất
         * * @return ResponseEntity
         * 
         * @throws IdInvalidException
         */
        @PostMapping("/auth/logout")
        @ApiMessage("Logout User")
        public ResponseEntity<Void> logout() throws IdInvalidException { // Thay vì ResLoginDTO, trả về Void
                // Lấy email của user đang đăng nhập (từ Access Token)
                String email = securityUtil.getCurrentUserLogin().isPresent() ? securityUtil.getCurrentUserLogin().get()
                                : "";

                if (email.equals("")) {
                        // Thực tế, nếu Access Token không hợp lệ, request sẽ bị chặn bởi
                        // SecurityConfig
                        // trước khi tới được đây (trừ khi /auth/logout nằm trong whiteList, nhưng
                        // không nên)
                        throw new IdInvalidException("Access Token không hợp lệ");
                }
                // 1. Vô hiệu hóa Refresh Token:
                // Xóa (set null) Refresh Token trong DB.
                // Đây là bước quan trọng nhất của logout.
                this.userService.updateUserToken(null, email);

                // 2. Xóa Refresh Token ở client:
                // Tạo một cookie "giả" có tên y hệt, giá trị rỗng và maxAge = 0
                ResponseCookie deleteSpringCookie = ResponseCookie
                                .from("refresh_token", null) // Giá trị rỗng
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(0) // Hết hạn ngay lập tức -> trình duyệt sẽ tự xóa
                                .build();

                // 3. Trả về response 200 OK với header Set-Cookie để xóa cookie
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                                .body(null); // Không cần body
        }

        /**
         * API Đăng ký tài khoản mới
         * * @param postManUser Đối tượng User lấy từ request body.
         * 
         * @return Thông tin user vừa được tạo.
         * @throws IdInvalidException
         */
        @PostMapping("/auth/register")
        @ApiMessage("Register a new user")
        public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody User postManUser)
                        throws IdInvalidException {

                // 1. Kiểm tra xem email đã tồn tại chưa
                boolean isEmailExist = this.userService.isEmailExist(postManUser.getEmail());
                if (isEmailExist) {
                        throw new IdInvalidException(
                                        "Email " + postManUser.getEmail()
                                                        + " đã tồn tại, vui lòng sử dụng email khác.");
                }

                // 2. Mã hóa mật khẩu người dùng trước khi lưu
                String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
                postManUser.setPassword(hashPassword);

                // 3. Gọi service để tạo user
                User hoangtung = this.userService.handleCreateUser(postManUser);

                // 4. Trả về response 201 CREATED (thay vì 200 OK)
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(this.userService.convertToResCreateUserDTO(hoangtung));
        }
}
