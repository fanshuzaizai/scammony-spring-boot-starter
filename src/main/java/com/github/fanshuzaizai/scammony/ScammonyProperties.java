package com.github.fanshuzaizai.scammony;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fanshuzaizai.
 * @date 2019/5/8 17:29
 */
@ConfigurationProperties(prefix = "scammony")
public class ScammonyProperties {

    /**
     * redis中锁的前缀
     */
    private String lockKeyPrefix = "scammony_lock_";

    /**
     * redis中存放任务执行标记的key
     * hash结构
     */
    private String markKey = "scammony_mark";

    private Global global = new Global();

    public static class Global {

        /**
         * 当前服务的id
         * 可以通过设置 {@<code> scammony.global.setting.${id}.run=false </code>}
         * 关闭使用了{@link MethodSwitch}的任务
         */
        private String id;

        /**
         * 如果{@link Global#id} 为空,会用当前值作为key，去尝试获取值作为id
         *
         * <p>
         * example: 已下设置同样会关闭任务
         * {@<code>
         * server.port = 8888
         * scammony.global.idKey=server.port
         * scammony.global.setting.8888.run=false
         * </code>}
         * </p>
         */
        private String idKey = "uid.datacenterId";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIdKey() {
            return idKey;
        }

        public void setIdKey(String idKey) {
            this.idKey = idKey;
        }
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        this.lockKeyPrefix = lockKeyPrefix;
    }

    public String getMarkKey() {
        return markKey;
    }

    public void setMarkKey(String markKey) {
        this.markKey = markKey;
    }

    public Global getGlobal() {
        return global;
    }

    public void setGlobal(Global global) {
        this.global = global;
    }
}
