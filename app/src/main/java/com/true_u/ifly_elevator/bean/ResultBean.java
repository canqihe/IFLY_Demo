package com.true_u.ifly_elevator.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Colin
 * on 2020/4/27
 * E-mail: hecanqi168@gmail.com
 * Copyright (C) 2018 SSZB, Inc.
 */
public class ResultBean implements Serializable {

    /**
     * sn : 1
     * ls : true
     * bg : 0
     * ed : 0
     * ws : [{"bg":0,"cw":[{"id":65535,"w":"去","gm":0,"sc":83}],"slot":"<takeTo>"},{"bg":0,"cw":[{"id":65535,"w":"6","gm":0,"sc":100}],"slot":"<num>"},{"bg":0,"cw":[{"id":65535,"w":"楼","gm":0,"sc":100}],"slot":"<contact>"}]
     * sc : 100
     */

    public int sn;
    public boolean ls;
    public int bg;
    public int ed;
    public int sc;
    public List<WsBean> ws;

    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }

    public boolean isLs() {
        return ls;
    }

    public void setLs(boolean ls) {
        this.ls = ls;
    }

    public int getBg() {
        return bg;
    }

    public void setBg(int bg) {
        this.bg = bg;
    }

    public int getEd() {
        return ed;
    }

    public void setEd(int ed) {
        this.ed = ed;
    }

    public int getSc() {
        return sc;
    }

    public void setSc(int sc) {
        this.sc = sc;
    }

    public List<WsBean> getWs() {
        return ws;
    }

    public void setWs(List<WsBean> ws) {
        this.ws = ws;
    }

    public static class WsBean implements Serializable {
        /**
         * bg : 0
         * cw : [{"id":65535,"w":"去","gm":0,"sc":83}]
         * slot : <takeTo>
         */

        public int bg;
        public String slot;
        public List<CwBean> cw;

        public int getBg() {
            return bg;
        }

        public void setBg(int bg) {
            this.bg = bg;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public List<CwBean> getCw() {
            return cw;
        }

        public void setCw(List<CwBean> cw) {
            this.cw = cw;
        }

        public static class CwBean {
            /**
             * id : 65535
             * w : 去
             * gm : 0
             * sc : 83
             */

            public int id;
            public String w;
            public int gm;
            public int sc;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getW() {
                return w;
            }

            public void setW(String w) {
                this.w = w;
            }

            public int getGm() {
                return gm;
            }

            public void setGm(int gm) {
                this.gm = gm;
            }

            public int getSc() {
                return sc;
            }

            public void setSc(int sc) {
                this.sc = sc;
            }
        }
    }
}
