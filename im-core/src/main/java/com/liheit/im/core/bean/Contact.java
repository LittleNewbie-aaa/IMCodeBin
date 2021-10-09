package com.liheit.im.core.bean;
import android.text.TextUtils;
import java.io.Serializable;

/**
 * 账号表
 * @author MaxRocky
 *
 */
@SuppressWarnings("serial")
//@DatabaseTable(tableName = "Contact")
public class Contact implements Serializable{
//	@DatabaseField(columnName="_id",generatedId=true)
    public int _id;
//	@DatabaseField(columnName="fullName")
	public String all_name;/*全名*/
//	@DatabaseField(columnName="fullPinYinName")
	public String person_id_mdm;/*拼音全称*/
//	@DatabaseField(columnName="userID",index=true,indexName="userID")
	public int id;
//	@DatabaseField(columnName="updateTime")
	public String update_time;
//	@DatabaseField(columnName="firstName")
	public String first_name;/*名字*/
//	@DatabaseField(columnName="lastName")
	public String last_name;/*姓*/
//	@DatabaseField(columnName="gender")
	public int gender;/*性别*/     //0 未知  1 男 2 女
//	@DatabaseField(columnName="birthday")
	public String birthday;/*生日*/
//	@DatabaseField(columnName="email")
	public String email;/*邮箱*/
//	@DatabaseField(columnName="workPhone")
	public String tel;/*座机*/
//	@DatabaseField(columnName="phone")
	public String mobile;/*手机*/
//	@DatabaseField(columnName="sign")
	public String sign;/*个人状态*/
//	@DatabaseField(columnName="address")
	public String addr;/*办公地点*/
//	@DatabaseField(columnName="userIcon")
	public String avatar;/*头像*/
//	@DatabaseField(columnName="status")
	public int status;
//	@DatabaseField(columnName="deptID")
	public String dept_id;
//	@DatabaseField(columnName="deptName")
	public String dept_name;/*部门名称*/
//	@DatabaseField(columnName="docsubject")
	public String title;/*岗位*/
//	@DatabaseField(columnName="post")
	public String post;/*职务*/
//	@DatabaseField(columnName="isDelete")
	public int is_delete;/*是否已删除*/
//	@DatabaseField(columnName="deleteTime")
	public String delete_time;/*删除时间*/
//	@DatabaseField(columnName="jobLevel")
	public int job_level;/*职务等级，用来排序*/
//	@DatabaseField(columnName="userIconBig")
	public String avatar_big;/*大头像*/
//	@DatabaseField(columnName="person_code_mdm")
	public String person_code_mdm;
//	@DatabaseField(columnName="unavailable")
	public int unavailable;
//	@DatabaseField(columnName="imkey",index=true,indexName="imkey")
	public int imkey;
//	@DatabaseField(columnName="orderby")
	public long orderby;
//	@DatabaseField(columnName="order_by_dept")
	public String order_by_dept;
//	@DatabaseField(columnName="wechat_num")
	public String wechat_num;
//	@DatabaseField(columnName="is_show_wechat")
	public int is_show_wechat;//1显示，0不显示
//	@DatabaseField(columnName="work_age")
	public int work_age;//司龄

	public String title_id;
	public String dept_id_mdm;
	public String additional_01;
	public String additional_02;
	public String additional_03;
	public boolean flagChoose;
	public Contact() {
		super();
	}

	public int getDeptID(){
		int deptID = 735;
		if(!TextUtils.isEmpty(dept_id)){
			String [] deptids = dept_id.split("@");
			if(deptids.length > 1){
				deptID = Integer.parseInt(deptids[1].toString());
			}else{
				deptID = Integer.parseInt(dept_id);
			}
		}
		if(deptID == 0){
			deptID = 735;
		}
		return deptID;
	}

	@Override
	public String toString() {
		return "Contact{" +
				"_id=" + _id +
				", all_name='" + all_name + '\'' +
				", person_id_mdm='" + person_id_mdm + '\'' +
				", id=" + id +
				", update_time='" + update_time + '\'' +
				", first_name='" + first_name + '\'' +
				", last_name='" + last_name + '\'' +
				", gender=" + gender +
				", birthday='" + birthday + '\'' +
				", email='" + email + '\'' +
				", tel='" + tel + '\'' +
				", mobile='" + mobile + '\'' +
				", sign='" + sign + '\'' +
				", addr='" + addr + '\'' +
				", avatar='" + avatar + '\'' +
				", status=" + status +
				", dept_id='" + dept_id + '\'' +
				", dept_name='" + dept_name + '\'' +
				", docsubject='" + title + '\'' +
				", is_delete=" + is_delete +
				", delete_time='" + delete_time + '\'' +
				", job_level=" + job_level +
				", avatar_big='" + avatar_big + '\'' +
				", person_code_mdm='" + person_code_mdm + '\'' +
				", unavailable=" + unavailable +
				", imkey=" + imkey +
				", orderby=" + orderby +
				", title_id='" + title_id + '\'' +
				", dept_id_mdm='" + dept_id_mdm + '\'' +
				", additional_01='" + additional_01 + '\'' +
				", additional_02='" + additional_02 + '\'' +
				", additional_03='" + additional_03 + '\'' +
				", flagChoose=" + flagChoose +
				'}';
	}
}
