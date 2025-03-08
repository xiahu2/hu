package com.yupi.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import cn.hutool.log.Log;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 定义流程
 */
@Slf4j
@Component
public abstract class JavaCodeSandboxTemplete implements CodeSandbox{

        private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

        private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

        private static final long TIME_OUT = 5000L;

        
    /**
     * 1.把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */

    private File saveCodeToFile(String code){

            // 1. 把用户的代码保存为文件

            String userDir = System.getProperty("user.dir");
            String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
            // 判断全局代码目录是否存在，没有则新建
            if (!FileUtil.exist(globalCodePathName)) {
                FileUtil.mkdir(globalCodePathName);
            }

            // 把用户的代码隔离存放
            String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
             String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
            File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

            return userCodeFile;
        }

    /**
     * 2. 编译代码，得到 class 文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){
        //        2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
            if(executeMessage.getExitValue()!=0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            //return getErrorResponse(e);
            throw new RuntimeException(e);
        }


    }

    /**
     * // 3. 执行代码，得到输出结果
     * @param inputList
     * @return
     */
    public  List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList ){
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);

            } catch (Exception e) {
               // return getErrorResponse(e);
                throw new RuntimeException("程序执行异常"+e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 收集整理输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputReponse(List<ExecuteMessage> executeMessageList){

        //        4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();

        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }

    /**
     * 5. 文件清理
     * @param userCodeFile
     * @return
     */
    public  boolean deleteFile(File userCodeFile){
//        5. 文件清理
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }


        @Override
        public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {


            List<String> inputList = executeCodeRequest.getInputList();
            String code = executeCodeRequest.getCode();
            String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件

            File userCodeFile = saveCodeToFile(code);

//        2. 编译代码，得到 class 文件
            ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
            System.out.println("编译日志 ："+ compileFileExecuteMessage);

//        3. 执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile,inputList);

//        4. 收集整理输出结果
            ExecuteCodeResponse executeCodeResponse = getOutputReponse(executeMessageList);


//        5. 文件清理
            boolean deleted = deleteFile(userCodeFile);
            if(BooleanUtil.isFalse(deleted)){
                log.error("文件清理失败 userCodeFilePath= {}" , userCodeFile.getAbsolutePath());
            }
            return executeCodeResponse;
        }

        /**
         * 6.获取错误响应
         *
         * @param e
         * @return
         */
        private ExecuteCodeResponse getErrorResponse(Throwable e) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setOutputList(new ArrayList<>());
            executeCodeResponse.setMessage(e.getMessage());
            // 表示代码沙箱错误
            executeCodeResponse.setStatus(2);
            executeCodeResponse.setJudgeInfo(new JudgeInfo());
            return executeCodeResponse;
        }


}
