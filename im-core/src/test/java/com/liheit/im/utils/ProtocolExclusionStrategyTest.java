package com.liheit.im.utils;

import com.liheit.im.core.bean.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

/**
 * Created by daixun on 2018/7/14.
 */
public class ProtocolExclusionStrategyTest {


    @Test
    public void shouldSkipField() throws Exception {
        Gson gson=new GsonBuilder().setExclusionStrategies(new ProtocolExclusionStrategy()).create();
        String json="{\"flag\":1,\"from\":\"tdaixun\",\"fromid\":900009,\"mid\":\"{0B142119-C754-4095-B528-DBFD00455FDE}\",\"msgs\":[{\"bytes\":383618,\"id\":0,\"isupload\":1,\"key\":\"\",\"localPath\":\"/storage/emulated/0/Android/data/com.dx.im/files/Download/temp/video_1531551982990.mp4\",\"md5\":\"666e5a190caf77cee28ef2a272e3811e\",\"mtype\":6,\"name\":\"\",\"sizeh\":0,\"sizew\":0,\"status\":0,\"t\":1,\"text\":\"\",\"token\":\"666e5a190caf77cee28ef2a272e3811e\"}],\"name\":\"代勋\",\"sendStatus\":0,\"sid\":\"{000DBBA3-0000-0000-0000-0000000DBBA9}\",\"t\":1531551987,\"toid\":900003,\"type\":0,\"utime\":\"\"}";

        ChatMessage msg = gson.fromJson(json,ChatMessage.class);

        System.out.println(msg);
        System.out.println(gson.toJson(msg));
    }


    @Test
    public void testPwdDicGen(){
       /*dict: abDBHlc7]4J~x}6 psw: BDHc]Jx6bBl74~}
        dict: +=-BHCc{]4}~Ffm 	psw: +]Hc]}Fm=BC{4~f
        dict: a=DFHCc{Z4J5x}m 	psw: aDHcZJxm=FC{45}
        dict: ab-FbCc{Z4}~x}m 	psw: a-bcZ}xmbFC{4~}
        dict: +bDFHCQ{Z4}5F}m 	psw: +DHQZ}FmbFC{45}
        dict: +b-FHlQ{]4}~xf6 	psw: F-HQ]}x6bFl{4~f
        dict: +=-FblQ7Z4}~xfm 	psw: +ZbQZ}xm=Fl74~f*/
        String dir="+b-Bblc7Z4J5F}6";
        System.out.println(calculatePwd(dir.getBytes()));
    }

    public static String calculatePwd(byte[] pszDict){
        /*bool IMGetZipKey(const char* pszDict, char aszPsw[ZipPswMax]) {
            memset(aszPsw,0,ZipPswMax);
            if (pszDict==NULL || strlen(pszDict)==0)
                return false;

            int nLen=strlen(pszDict);
            int nCrc=0;
            for (int i=0; i<nLen; i++) {	// 使用移位累加计算CRC
                nCrc<<= 1;			// 左移一位
                nCrc |= pszDict[i];	// 或上字典值
            }

            for (int n=0; n<nLen; n++) {	// 生成zip密码
                if ((nCrc>>n)&1)
                    aszPsw[n]=pszDict[(n<<1)%nLen];
                else
                    aszPsw[n]=pszDict[((n+1)*(n+3))%nLen];
            }

            return true;
        }*/

        int nLen=pszDict.length;
        byte[] aszPsw=new byte[nLen];
        int nCrc=0;
        for (int i=0; i<nLen; i++) {	// 使用移位累加计算CRC
            nCrc<<= 1;			// 左移一位
            nCrc |= pszDict[i];	// 或上字典值
        }
        for (int n=0; n<nLen; n++) {	// 生成zip密码
            if (((nCrc>>n)&1)==1)
                aszPsw[n]=pszDict[(n<<1)%nLen];
            else
                aszPsw[n]=pszDict[((n+1)*(n+3))%nLen];
        }

        return new String(aszPsw);
    }

}