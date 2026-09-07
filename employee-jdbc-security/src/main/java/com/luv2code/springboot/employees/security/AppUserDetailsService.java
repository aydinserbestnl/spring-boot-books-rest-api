package com.luv2code.springboot.employees.security;

import com.luv2code.springboot.employees.dao.UserDAO;
import com.luv2code.springboot.employees.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/*
JdbcUserDetailsManager'a alternatif: kendi UserDAO'muzu (duz JdbcTemplate) kullanarak
kullaniciyi ve rollerini biz kendimiz cekiyoruz. Sifre karsilastirmasini yine
DaoAuthenticationProvider ve PasswordEncoder (BCrypt) otomatik yapiyor.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    public AppUserDetailsService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = userDAO.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = userDAO.findRolesByUsername(username)
                .stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        return new User(appUser.username(), appUser.password(), appUser.active(),
                true, true, true, authorities);
    }
}
