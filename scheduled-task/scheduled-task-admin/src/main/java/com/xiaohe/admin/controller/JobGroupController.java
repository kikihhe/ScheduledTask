package com.xiaohe.admin.controller;


import com.xiaohe.admin.core.model.XxlJobGroup;
import com.xiaohe.admin.core.model.XxlJobRegistry;
import com.xiaohe.admin.core.util.I18nUtil;
import com.xiaohe.admin.mapper.XxlJobGroupMapper;
import com.xiaohe.admin.mapper.XxlJobInfoMapper;
import com.xiaohe.admin.mapper.XxlJobRegistryMapper;
import com.xiaohe.core.enums.RegistryConfig;
import com.xiaohe.core.model.Result;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 该类对应的是执行器管理那个界面
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    @Resource
    public XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    public XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobRegistryMapper xxlJobRegistryMapper;

    @RequestMapping
    public String index(Model model) {
        return "jobgroup/jobgroup.index";
    }


    /**
     * 查询所有的执行器
     */
    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appname, String title) {
        List<XxlJobGroup> list = xxlJobGroupMapper.pageList(start, length, appname, title);
        int list_count = xxlJobGroupMapper.pageListCount(start, length, appname, title);
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);
        maps.put("recordsFiltered", list_count);
        maps.put("data", list);
        return maps;
    }


    /**
     * 新增一个执行器的方法
     */
    @RequestMapping("/save")
    @ResponseBody
    public Result<String> save(XxlJobGroup xxlJobGroup){
        // valid
        if (xxlJobGroup.getAppname()==null || xxlJobGroup.getAppname().trim().length()==0) {
            return new Result<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (xxlJobGroup.getAppname().length()<4 || xxlJobGroup.getAppname().length()>64) {
            return new Result<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (xxlJobGroup.getAppname().contains(">") || xxlJobGroup.getAppname().contains("<")) {
            return new Result<String>(500, "AppName"+I18nUtil.getString("system_unvalid") );
        }
        if (xxlJobGroup.getTitle()==null || xxlJobGroup.getTitle().trim().length()==0) {
            return new Result<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }
        if (xxlJobGroup.getTitle().contains(">") || xxlJobGroup.getTitle().contains("<")) {
            return new Result<String>(500, I18nUtil.getString("jobgroup_field_title")+I18nUtil.getString("system_unvalid") );
        }
        if (xxlJobGroup.getAddressType()!=0) {
            if (xxlJobGroup.getAddressList()==null || xxlJobGroup.getAddressList().trim().length()==0) {
                return new Result<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            if (xxlJobGroup.getAddressList().contains(">") || xxlJobGroup.getAddressList().contains("<")) {
                return new Result<String>(500, I18nUtil.getString("jobgroup_field_registryList")+I18nUtil.getString("system_unvalid") );
            }
            String[] addresss = xxlJobGroup.getAddressList().split(",");
            for (String item: addresss) {
                if (item==null || item.trim().length()==0) {
                    return new Result<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid") );
                }
            }
        }
        xxlJobGroup.setUpdateTime(new Date());
        int ret = xxlJobGroupMapper.save(xxlJobGroup);
        return (ret>0)?Result.SUCCESS:Result.FAIL;
    }


    /**
     * 更新执行器的方法
     */
    @RequestMapping("/update")
    @ResponseBody
    public Result<String> update(XxlJobGroup xxlJobGroup) {
        if (xxlJobGroup.getAppname()==null || xxlJobGroup.getAppname().trim().length()==0) {
            return new Result<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (xxlJobGroup.getAppname().length()<4 || xxlJobGroup.getAppname().length()>64) {
            return new Result<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (xxlJobGroup.getTitle()==null || xxlJobGroup.getTitle().trim().length()==0) {
            return new Result<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }
        //分自动注册和手动注册，0为自动注册，1为手动注册
        if (xxlJobGroup.getAddressType() == 0) {
            List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppname());
            String addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = "";
                for (String item : registryList) {
                    addressListStr += item + ",";
                }
                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
            }
            xxlJobGroup.setAddressList(addressListStr);
        } else {
            if (xxlJobGroup.getAddressList() == null || xxlJobGroup.getAddressList().trim().length() == 0) {
                return new Result<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            String[] addresss = xxlJobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item == null || item.trim().length() == 0) {
                    return new Result<String>(500,  I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }
        xxlJobGroup.setUpdateTime(new Date());
        int ret = xxlJobGroupMapper.update(xxlJobGroup);
        return (ret > 0) ? Result.SUCCESS : Result.FAIL;
    }


    /**
     * 根据执行器名称，也就是appName来查询执行器的方法
     */
    private List<String> findRegistryByAppName(String appnameParam) {
        HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
        //这里查询出的执行器是没有超时的，超时的就不会被查到了
        List<XxlJobRegistry> list = xxlJobRegistryMapper.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (list != null) {
            for (XxlJobRegistry item : list) {
                //这里查找的是自动注册的执行器
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appname = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appname);
                    if (registryList == null) {
                        registryList = new ArrayList<String>();
                    }
                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appname, registryList);
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }


    /**
     * 删除执行器的方法，根据执行器的id
     */
    @RequestMapping("/remove")
    @ResponseBody
    public Result<String> remove(int id) {
        int count = xxlJobInfoMapper.pageListCount(0, 10, id, -1, null, null, null);
        if (count > 0) {
            return new Result<String>(500, I18nUtil.getString("jobgroup_del_limit_0") );
        }
        List<XxlJobGroup> allList = xxlJobGroupMapper.findAll();
        if (allList.size() == 1) {
            return new Result<String>(500, I18nUtil.getString("jobgroup_del_limit_1") );
        }
        int ret = xxlJobGroupMapper.remove(id);
        return (ret > 0) ? Result.SUCCESS : Result.FAIL;
    }


    /**
     * 根据id查找执行器的方法
     */
    @RequestMapping("/loadById")
    @ResponseBody
    public Result<XxlJobGroup> loadById(int id) {
        XxlJobGroup jobGroup = xxlJobGroupMapper.load(id);
        return jobGroup != null ? new Result<XxlJobGroup>(jobGroup) : new Result<XxlJobGroup>(Result.FAIL_CODE, null);
    }

}
