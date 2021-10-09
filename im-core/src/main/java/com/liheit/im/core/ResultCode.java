package com.liheit.im.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by daixun on 2018/6/15.
 */

public class ResultCode {


    // 错误码定义(满足Windows 的HRESULT)，负数是出错
    // 客户端定义的错误码 +200（200以内是服务器所定义）
    // 结果类型
    public static final long erSuccess = 0x00000000L;	// 成功
    public static final long erInfo    = 0x60000000L;	// 为信息
    public static final long erWarn    = 0xA0000000L;	// 警告(最高位为1)
    public static final long erError   = 0xE0000000L;	// 错误(最高位为1)
    public static final long erClient  = 0x200L;		// 客户端定义错误码的起始值

    // 结果分类(Family)
    public static final long erCommon  = erError + 0x00010000L;	// 通用错误
    public static final long erIM      = erError + 0x00020000L;	// IM相关的结果
    public static final long erDB      = erError + 0x00030000L;	// Database相关的结果
    public static final long erNet     = erError + 0x00040000L;	// 网络错误
    public static final long erDevice  = erError + 0x00050000L;	// 设备相关错误
    public static final long erSSO     = erError + 0x00060000L;	// SSO错误
    public static final long erEngine  = erError + 0x00070000L;	// 引擎错误
    public static final long erUpdater = erError + 0x00080000L;	// 升级程序错误
    public static final long erCurl    = erError + 0x00090000L;	// cUrl错误
    public static final long erHttp    = erError + 0x000a0000L;	// HTTP(s)错误
    public static final long erWebapp  = erError + 0x000b0000L;	// 轻应用错误
    public static final long erMeeting = erError + 0x000c0000L;	// 会议错误


    // 通用错误码 ===================================================
    public static final long ErrOK				= erSuccess;	// 成功
    public static final long ErrFail				= 0x80004005L;	// 未指定错误
    public static final long ErrNotImpl			= 0x80004001L;	// 功能未实现
    public static final long ErrNoInterface		= 0x80004002L;	// 接口未实现
    public static final long ErrInvalidPointer	= 0x80004003L;	// 无效指针
    public static final long ErrInvalidParam		= 0x80070057L;	// 无效参数
    public static final long ErrInvalidHandle	= 0x80070006L;	// 无效句柄
    public static final long ErrOutOfMemory		= 0x8007000EL;	// 内存溢出
    public static final long ErrAccessDenied		= 0x80070005L;	// 拒绝访问

    public static final long ErrUnknown           = erCommon+1;  // 未知错误
    public static final long ErrNULL              = erCommon+2;  // <nil>
    public static final long ErrSendError         = erCommon+3;  // 发送数据出错
    public static final long ErrOverload          = erCommon+4;  // 超过负荷，稍后重试
    public static final long ErrAccount           = erCommon+5;  // 无效账号
    public static final long ErrPassword          = erCommon+6;  // 密码错误
    public static final long ErrToken             = erCommon+7;  // Token无效
    public static final long ErrKicked            = erCommon+8;  // 被踢
    public static final long ErrForbidden         = erCommon+9;  // 账号被禁用
    public static final long ErrNoSession         = erCommon+10; // 会话不存在
    public static final long ErrUserID            = erCommon+11; // 用户ID无效
    public static final long ErrNoUserInfo        = erCommon+12; // 没有用户信息
    public static final long ErrModifyFlag        = erCommon+13; // 没有设置用户更新标识
    public static final long ErrModifyFailed      = erCommon+14; // 更新用户信息失败
    public static final long ErrNoReceiver        = erCommon+15; // P2P会话没有接收者
    public static final long ErrSessionMember     = erCommon+16; // 没有会话成员
    public static final long ErrZipFile           = erCommon+17; // 创建ZIP文件失败
//	public static final long ErrNoDeptInfo        = erCommon+18; // 没有部门信息
//	public static final long ErrNoOfflineMsg      = erCommon+19; // 没有离线消息
//	public static final long ErrNoMessages        = erCommon+20; // 没有消息记录
    public static final long ErrSessionExisted    = erCommon+21; // 会话已经存在
    public static final long ErrSessionSingle     = erCommon+22; // 单聊群不用创建
//  public static final long ErrSessionDept       = erCommon+23; // 部门群不用创建
//	public static final long ErrSessionDiss       = erCommon+24; // 讨论组不用创建
    public static final long ErrSessionMemberNoMe = erCommon+25; // 群成员中没有创建者
    public static final long ErrSessionMemberLT3  = erCommon+26; // 群成不能少于3人
    public static final long ErrSessionType       = erCommon+27; // 无效会话类型
    public static final long ErrSessionPermission = erCommon+28; // 无权限修改群信息
    public static final long ErrSessionTo         = erCommon+29; // 群转让的接收者不是群成员
    public static final long ErrSessionOwner      = erCommon+30; // 不能删除群主
    public static final long ErrSessionNoSender   = erCommon+31; // 非成员不能发送消息
    public static final long ErrMyData            = erCommon+32; // 无效个人数据
    public static final long ErrSessionMemberExceed=erCommon+33; // 群成员超过上限
    public static final long ErrNoConfig          = erCommon+34; // 没有相关配置


    // 数据库错误码 =================================================
    public static final long ErrDbNotOpen= erDB+1;	// 数据库未打开
    public static final long ErrDbWriteDb= erDB+2;	// 写数据库失败
    public static final long ErrQueryDb  = erDB+3;	// 查询数据库失败
    public static final long ErrReadDb   = erDB+4;	// 读取数据库失败


    // 网络错误码 ===================================================
    public static final long ErrNetPackage		= erNet+1;		// 数据包错误
    public static final long ErrNetIncomplete	= erNet+2;		// 数据包未接收完整，继续接收
    public static final long ErrNetCRC			= erNet+3;		// CRC校验失败
    public static final long ErrNetPack			= erNet+4;		// 数据打包出错
    public static final long ErrNetNotConnWebapp = erNet+5;		// 没有连接到推送服务器
    public static final long ErrNetBadRequest    = erNet+6;		// 无效请求
    public static final long ErrJSONFormat		= erNet+7;		// JSON格式错误


    // SSO 错误 ======================================================
    public static final long ErrSSO                 = erSSO+1; // SSO错误
    public static final long ErrSSOBadRequest       = erSSO+2; // 无效请求(10001)
    public static final long ErrSSONoResource       = erSSO+3; // 没有找到对应资源(10002)
    public static final long ErrSSOBadParams        = erSSO+4; // 缺少请求参数(10003)
    public static final long ErrSSOInvalidReq       = erSSO+5; // 无效请求参数(10004)
    public static final long ErrSSOBadHetero        = erSSO+6; // 无效异构系统(10005)
    public static final long ErrSSOBadheteroIP      = erSSO+7; // 无效异构系统IP(10006)
    public static final long ErrSSOResource         = erSSO+8; // 资源错误(10007)
    public static final long ErrSSOBadAccount       = erSSO+9; // 账号错误(10101)
    public static final long ErrSSOBadPassword      = erSSO+10;// 密码错误(10102)
    public static final long ErrSSOAccountPsw       = erSSO+11;// 账号或者密码错误(10103)
    public static final long ErrSSOPswExpired       = erSSO+12;// 密码已经过期(10104)
    public static final long ErrSSONoPermissions    = erSSO+13;// 没有权限(99997)
    public static final long ErrSSOHeteroSystem     = erSSO+14;// HTTP请求异构系统异常(99998)
    public static final long ErrSSOServerExceptions = erSSO+15;// 服务器异常(99999)





    public static final long ERR_UNKNOWN=erClient+1;//未知错误
    public static final long NEED_UPGRADE_CLIENT = erClient + 2;//需要强制更新
    public static final long CONNECTION_INTERRUPTION = erClient + 3;//网络连接中断

    private static final Map<Long, String> codeMapping = new HashMap<>();

    static {
        codeMapping.put(erSuccess ," 成功");
        codeMapping.put(erInfo    ," 为信息");
        codeMapping.put(erWarn    ," 警告(最高位为1)");
        codeMapping.put(erError   ," 错误(最高位为1)");
        codeMapping.put(erClient,"客户端定义错误码的起始值");
        // 结果分类(Family)
        codeMapping.put(erCommon  ," 通用错误");
        codeMapping.put(erIM      ," IM相关的结果");
        codeMapping.put(erDB      ," Database相关的结果");
        codeMapping.put(erNet     ," 网络错误");
        codeMapping.put(erDevice  ," 设备相关错误");
        codeMapping.put(erSSO     ," SSO错误");
        codeMapping.put(erEngine  ," 引擎错误");
        codeMapping.put(erUpdater ," 升级程序错误");
        codeMapping.put(erCurl    ," cUrl错误");
        codeMapping.put(erHttp    ," HTTP(s)错误");
        codeMapping.put(erWebapp  ," 轻应用错误");
        codeMapping.put(erMeeting ," 会议错误");
        // 通用错误码 ===================================================
        codeMapping.put(ErrOK				," 成功");
        codeMapping.put(ErrFail				," 未指定错误");
        codeMapping.put(ErrNotImpl			," 功能未实现");
        codeMapping.put(ErrNoInterface		," 接口未实现	");
        codeMapping.put(ErrInvalidPointer	," 无效指针");
        codeMapping.put(ErrInvalidParam		," 无效参数");
        codeMapping.put(ErrInvalidHandle	," 无效句柄");
        codeMapping.put(ErrOutOfMemory		," 内存溢出");
        codeMapping.put(ErrAccessDenied		," 拒绝访问");
        codeMapping.put(ErrUnknown           ,"未知错误1");
        codeMapping.put(ErrNULL              ," <nil>");
        codeMapping.put(ErrSendError         ," 发送数据出错");
        codeMapping.put(ErrOverload          ," 超过负荷，稍后重试");
        codeMapping.put(ErrAccount           ," 无效账号");
        codeMapping.put(ErrPassword          ," 密码错误");
        codeMapping.put(ErrToken             ," Token无效");
        codeMapping.put(ErrKicked            ," 被踢");
        codeMapping.put(ErrForbidden         ," 账号被禁用");
        codeMapping.put(ErrNoSession         ," 会话不存在");
        codeMapping.put(ErrUserID            ," 用户ID无效");
        codeMapping.put(ErrNoUserInfo        ," 没有用户信息");
        codeMapping.put(ErrModifyFlag        ," 没有设置用户更新标识");
        codeMapping.put(ErrModifyFailed      ," 更新用户信息失败");
        codeMapping.put(ErrNoReceiver        ," P2P会话没有接收者");
        codeMapping.put(ErrSessionMember     ," 没有会话成员");
        codeMapping.put(ErrZipFile           ," 创建ZIP文件失败");
        codeMapping.put(ErrSessionExisted    ," 会话已经存在");
        codeMapping.put(ErrSessionSingle     ," 单聊群不用创建");
//        codeMapping.put(ErrSessionDept       ," 部门群不用创建");
        codeMapping.put(ErrSessionMemberNoMe ," 群成员中没有创建者");
        codeMapping.put(ErrSessionMemberLT3  ," 群成不能少于3人");
        codeMapping.put(ErrSessionType       ," 无效会话类型");
        codeMapping.put(ErrSessionPermission ," 无权限修改群信息");
        codeMapping.put(ErrSessionTo         ," 群转让的接收者不是群成员");
        codeMapping.put(ErrSessionOwner      ," 不能删除群主");
        codeMapping.put(ErrSessionNoSender   ,"非成员不能发送消息");
        codeMapping.put(ErrMyData             ,"无效个人数据");
        codeMapping.put(ErrSessionMemberExceed,"群成员超过上限");
        codeMapping.put(ErrNoConfig           ,"没有相关配置");

        // 数据库错误码 =================================================
        codeMapping.put(ErrDbNotOpen,"数据库未打开");

        codeMapping.put(ErrDbWriteDb,"写数据库失败");
        codeMapping.put(ErrQueryDb  ,"查询数据库失败");
        codeMapping.put(ErrReadDb   ,"读取数据库失败");


        // 网络错误码 ===================================================
        codeMapping.put(ErrNetPackage	,"数据包错误");
        codeMapping.put(ErrNetIncomplete,"数据包未接收完整，继续接收");
        codeMapping.put(ErrNetCRC	,"CRC校验失败");
        codeMapping.put(ErrNetPack	,"数据打包出错");
        codeMapping.put(ErrNetNotConnWebapp ,"没有连接到推送服务器");
        codeMapping.put(ErrNetBadRequest  ,"无效请求");
        codeMapping.put(ErrJSONFormat		,"JSON格式错误");
        // SSO 错误 ======================================================
        codeMapping.put(ErrSSO ,"SSO错误");

        codeMapping.put(ErrSSOBadRequest       ,"无效请求(10001)");
        codeMapping.put(ErrSSONoResource       ,"没有找到对应资源(10002)");
        codeMapping.put(ErrSSOBadParams        ,"缺少请求参数(10003)");
        codeMapping.put(ErrSSOInvalidReq       ,"无效请求参数(10004)");
        codeMapping.put(ErrSSOBadHetero        ,"无效异构系统(10005)");
        codeMapping.put(ErrSSOBadheteroIP      ,"无效异构系统IP(10006)");
        codeMapping.put(ErrSSOResource         ,"资源错误(10007)");
        codeMapping.put(ErrSSOBadAccount       ,"账号错误(10101)");
        codeMapping.put(ErrSSOBadPassword      ,"密码错误(10102)");
        codeMapping.put(ErrSSOAccountPsw       ,"账号或者密码错误(10103)");
        codeMapping.put(ErrSSOPswExpired       ,"密码已经过期(10104)");
        codeMapping.put(ErrSSONoPermissions    ,"没有权限(99997)");
        codeMapping.put(ErrSSOHeteroSystem     ,"HTTP请求异构系统异常(99998)");
        codeMapping.put(ErrSSOServerExceptions ,"服务器异常(99999)");



//        codeMapping.put(SEND_ERROR,"发送失败");


        codeMapping.put(ERR_UNKNOWN,"未知错误2");
        codeMapping.put(NEED_UPGRADE_CLIENT,"请升级客户端");
        codeMapping.put(CONNECTION_INTERRUPTION,"网络连接中断");
    }

    public static String formatResultCode(long code) {
        String str = codeMapping.get(code);
        return str == null ? (""+code) : str;
    }

}
