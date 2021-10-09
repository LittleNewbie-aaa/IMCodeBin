package com.liheit.im.core.link;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.liheit.im.common.ext.AppNameFlag;
import com.liheit.im.common.ext.CommonExtKt;
import com.liheit.im.core.bean.Contact;

/**
 * 用于通过contentprovider 从华远中获取他们的通讯录信息
 */
public class LinkUtil {
    /**
     * 测试从contentprovider中拿到用户的数据
     */
    private static String myURIstr;

    public static Contact test(Context context, long userid) {
        myURIstr = "content://" +context.getPackageName()+".MyContentProvider/Contact/%s";

        //构建uri
        Uri uri = Uri.parse(String.format(myURIstr, userid));
//        Log.e("aaa"+uri.toString());
        Cursor c = null;
        try{
          c = context.getApplicationContext().getContentResolver().query(uri, null, null, null, null); 
        }catch (Exception e){
            
        }
        Contact contact = new Contact();
        if (c != null && c.moveToNext()) {
            contact.all_name = c.getString(c.getColumnIndex("fullName"));
            contact.person_id_mdm = c.getString(c.getColumnIndex("fullPinYinName"));
            contact.first_name = c.getString(c.getColumnIndex("firstName"));
            contact.last_name = c.getString(c.getColumnIndex("lastName"));
            contact.gender = c.getInt(c.getColumnIndex("gender"));
            contact.birthday = c.getString(c.getColumnIndex("birthday"));
            contact.email = c.getString(c.getColumnIndex("email"));
            contact.tel = c.getString(c.getColumnIndex("workPhone"));
            contact.mobile = c.getString(c.getColumnIndex("phone"));
            contact.addr = c.getString(c.getColumnIndex("address"));
            contact.dept_id = c.getString(c.getColumnIndex("deptID"));
            contact.dept_name = c.getString(c.getColumnIndex("deptName"));
            if(CommonExtKt.getAppName()== AppNameFlag.THE_HY_FLAG.getValue()){
                contact.title = c.getString(c.getColumnIndex("title"));
            }else if(CommonExtKt.getAppName()== AppNameFlag.THE_SD_FLAG.getValue()){
                contact.title = c.getString(c.getColumnIndex("docsubject"));
            }
            contact.job_level = c.getInt(c.getColumnIndex("jobLevel"));
            contact.avatar = c.getString(c.getColumnIndex("userIcon"));
            contact.avatar_big = c.getString(c.getColumnIndex("userIconBig"));
        }
//        Log.e("userhead:" + contact.toString());
        return contact;
    }
}
