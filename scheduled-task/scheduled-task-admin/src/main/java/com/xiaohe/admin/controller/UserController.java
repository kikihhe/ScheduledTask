package com.xiaohe.admin.controller;

import com.xiaohe.admin.controller.annotation.PermissionLimit;
import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobUser;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.mapper.XxlJobUserMapper;
import com.xiaohe.admin.service.LoginService;
import com.xiaohe.biz.model.Result;
import com.xiaohe.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-22 15:02
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {
        List<XxlJobGroup> all = xxlJobGroupMapper.findAll();
        model.addAttribute("groupList", all);
        return "user/user.index";
    }

    /**
     * 分页查询用户
     * @param start
     * @param length
     * @param username
     * @param role
     * @return
     */
    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username,
                                        int role) {
        List<XxlJobUser> list = xxlJobUserMapper.pageList(start, length, username, role);
        // 不能将密码返回前端
        list.forEach(user -> {user.setPassword(null);});
        Map<String, Object> map = new HashMap<>();
        map.put("recordsTotal", list.size());
        map.put("recordsFiltered", list.size());
        map.put("data", list);
        return map;
    }

    /**
     * 添加新用户
     * @param xxlJobUser
     * @return
     */
    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Result<String> add(XxlJobUser xxlJobUser) {
        String username = xxlJobUser.getUsername();
        String password = xxlJobUser.getPassword();
        if (!StringUtil.hasText(username) || username.length() < 4 || username.length() > 20) {
            return new Result<String>(Result.FAIL_CODE, "用户名不符合格式, 不能为空，长度要在4-20之间");
        }
        if (!StringUtil.hasText(password) || password.length() < 4 || password.length() > 20) {
            return new Result<String>(Result.FAIL_CODE, "密码不符合格式, 不能为空，长度要在4-20之间");
        }
        xxlJobUser.setUsername(username.trim());
        xxlJobUser.setPassword(password.trim());
        XxlJobUser xxlJobUser1 = xxlJobUserMapper.loadByUsername(username);
        if (xxlJobUser1 != null) {
            return new Result<String>(Result.FAIL_CODE, "用户:" + username + "已存在，请勿重复添加");
        }
        xxlJobUserMapper.add(xxlJobUser);
        return Result.SUCCESS;
    }

    /**
     * 修改某个用户的信息
     * @param xxlJobUser
     * @return
     */
    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Result<String> update(@RequestBody XxlJobUser xxlJobUser, HttpServletRequest request) {
        XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        String newPassword = xxlJobUser.getPassword();
        if (!StringUtil.hasText(newPassword) || newPassword.length() < 4 || newPassword.length() > 20) {
            return new Result<>(Result.FAIL_CODE, "密码不符合格式");
        }
        xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes(StandardCharsets.UTF_8)));
        xxlJobUserMapper.update(xxlJobUser);
        return Result.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Result<String> remove(int id, HttpServletRequest request) {
        XxlJobUser xxlJobUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        xxlJobUserMapper.delete(id);
        return Result.SUCCESS;
    }

    /**
     * 修改密码
     * @param request
     * @param password
     * @return
     */
    @RequestMapping("/updatePwd")
    @ResponseBody
    public Result<String> updatePassword(HttpServletRequest request, String password) {
        if (!StringUtil.hasText(password)) {
            return new Result<>(Result.FAIL_CODE, "密码不能为空");
        }
        password = password.trim();
        if (password.length() < 4 || password.length() > 20) {
            return new Result<>(Result.FAIL_CODE, "密码长度要在 4 ~ 20 之间");
        }
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        XxlJobUser user = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        XxlJobUser xxlJobUser = xxlJobUserMapper.loadByUsername(user.getUsername());
        xxlJobUser.setPassword(md5Password);
        xxlJobUserMapper.update(xxlJobUser);
        return Result.SUCCESS;
    }



}
