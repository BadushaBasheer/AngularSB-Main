package com.demorest.usermanagement.controller;

import com.demorest.usermanagement.entity.Role;
import com.demorest.usermanagement.entity.User;
import com.demorest.usermanagement.entity.UserRole;
import com.demorest.usermanagement.service.RoleService;
import com.demorest.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.nio.file.Paths.get;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final RoleService roleService;
    public static final String DIRECTORY;

    static {
        try {
            DIRECTORY = new ClassPathResource("static/").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> registerNewUser(@RequestBody User user) {

        User user1 = user;

        User savedCustomer = null;
        ResponseEntity response = null;
        try {
            Role role = roleService.getRole();
            Set<UserRole> userRoles = new HashSet<>();
            UserRole userRole = new UserRole();
            userRole.setRole(role);
            userRole.setUser(user);
            userRoles.add(userRole);
            savedCustomer = userService.createUser(user, userRoles);
            if (savedCustomer.getId() > 0) {
                response = ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body("{\"message\": \"Given user details are successfully registered\"}");
            }

        } catch (Exception e) {
            response = ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("{\"message\": \"Username already present, please try with another\"}"));
        }
        return response;
    }


    @GetMapping("/{userName}")
    public User getUser(@PathVariable("userName") String userName) {
        return userService.getUser(userName);
    }

    @GetMapping("/current-user")
    public User getCurrentUser(Principal principal) {
        return userService.getUser(principal.getName());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile multipartFile, Principal principal) throws IOException {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        Path fileStorage = get(DIRECTORY, filename).toAbsolutePath().normalize();
        copy(multipartFile.getInputStream(), fileStorage, REPLACE_EXISTING);

        User user = userService.getUser(principal.getName());
        userService.updateImage(user, filename);

        return ResponseEntity.ok().body(filename);
    }

    @GetMapping("download/{filename}")
    public ResponseEntity<Resource> downloadFiles(@PathVariable("filename") String filename) throws IOException {
        Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(filename);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(filename + "was not found on the server");
        }
        Resource resource = new UrlResource(filePath.toUri());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("File-Name", filename);
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;File-Name=" + resource.getFilename());

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(Files.probeContentType(filePath)))
                .headers(httpHeaders).body(resource);
    }
}
