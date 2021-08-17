package com.INFO7255.Service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyUserDetailsService implements UserDetailsService {
    List<String[]> users = new ArrayList<>();
    String[] userNames = {"foo", "funcUser01", "funcUser02", "userTest03"};
    MyUserDetailsService(){
        for(String name : userNames){
            String[] temp = {name, name};
            users.add(temp);
        }
    }
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
       List<String[]> user =  users.stream().filter((arr) -> arr[0].equals(s)).collect(Collectors.toList());
       if(user.size() > 0){
           return new User(user.get(0)[0], user.get(0)[1], new ArrayList<>());
       }
       throw new UsernameNotFoundException("Invalid UserName " + s);
    }
}