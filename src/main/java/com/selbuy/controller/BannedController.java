package com.selbuy.controller;

import com.selbuy.model.AuctionLot;
import com.selbuy.model.AuctionTransaction;
import com.selbuy.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/banned")
@RequiredArgsConstructor
public class BannedController {
    @GetMapping("/userhome")
    public String userDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        return "banned/userhome";
    }
}