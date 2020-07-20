package com.true_u.ifly_elevator.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Colin
 * on 2020/7/20
 * E-mail: hecanqi168@gmail.com
 */
public class FloorBean implements Serializable {

    /**
     * data : [{"id":340,"logicalFloor":1,"numberFloor":-1,"plotDetailId":7,"showFloor":"-1楼"},{"id":341,"logicalFloor":2,"numberFloor":1,"plotDetailId":7,"showFloor":"1楼"},{"id":342,"logicalFloor":3,"numberFloor":2,"plotDetailId":7,"showFloor":"2楼"},{"id":343,"logicalFloor":4,"numberFloor":3,"plotDetailId":7,"showFloor":"3楼"},{"id":344,"logicalFloor":5,"numberFloor":4,"plotDetailId":7,"showFloor":"4楼"},{"id":345,"logicalFloor":6,"numberFloor":5,"plotDetailId":7,"showFloor":"5楼"},{"id":346,"logicalFloor":7,"numberFloor":6,"plotDetailId":7,"showFloor":"6楼"},{"id":347,"logicalFloor":8,"numberFloor":7,"plotDetailId":7,"showFloor":"7楼"},{"id":348,"logicalFloor":9,"numberFloor":8,"plotDetailId":7,"showFloor":"8楼"},{"id":349,"logicalFloor":10,"numberFloor":9,"plotDetailId":7,"showFloor":"9楼"},{"id":350,"logicalFloor":11,"numberFloor":10,"plotDetailId":7,"showFloor":"10楼"},{"id":351,"logicalFloor":12,"numberFloor":11,"plotDetailId":7,"showFloor":"11楼"}]
     * errorMsg : success
     * resultCode : 0
     */

    private String errorMsg;
    private int resultCode;
    private List<DataBean> data;

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * id : 340
         * logicalFloor : 1
         * numberFloor : -1
         * plotDetailId : 7
         * showFloor : -1楼
         */

        private int id;
        private int logicalFloor;
        private int numberFloor;
        private int plotDetailId;
        private String showFloor;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getLogicalFloor() {
            return logicalFloor;
        }

        public void setLogicalFloor(int logicalFloor) {
            this.logicalFloor = logicalFloor;
        }

        public int getNumberFloor() {
            return numberFloor;
        }

        public void setNumberFloor(int numberFloor) {
            this.numberFloor = numberFloor;
        }

        public int getPlotDetailId() {
            return plotDetailId;
        }

        public void setPlotDetailId(int plotDetailId) {
            this.plotDetailId = plotDetailId;
        }

        public String getShowFloor() {
            return showFloor;
        }

        public void setShowFloor(String showFloor) {
            this.showFloor = showFloor;
        }
    }
}
