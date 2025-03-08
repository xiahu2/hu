package com.yupi.yuojcodesandbox.controller;

import com.yupi.yuojcodesandbox.JavaNativeCodeSandbox;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Scanner;

@RestController
public class MainController {
    private static final String AUTH_REQUEST_HEADER ="auth";
    private static final String AUTH_REQUEST_SECRET ="secretKey";
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }


    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        String header = request.getHeader(AUTH_REQUEST_HEADER);
        if(!AUTH_REQUEST_SECRET.equals(header)){
            response.setStatus(403);
            return null;

        }
        if(executeCodeRequest ==null){

            throw  new RuntimeException("请求为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);

    }
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        String[] s = input.split(" ");
        int[] nums = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            nums[i] = Integer.parseInt(s[i]);
            System.out.println(nums[i]);
        }



    }

}
