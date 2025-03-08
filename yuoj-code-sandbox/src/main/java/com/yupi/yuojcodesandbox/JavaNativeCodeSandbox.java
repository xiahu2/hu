package com.yupi.yuojcodesandbox;

import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.context.annotation.Configuration;

/**
 * java 原生实现代码沙箱 直接继承模板方法
 */
@Configuration
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplete{

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }


}
