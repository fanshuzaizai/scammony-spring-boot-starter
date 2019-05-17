package com.github.fanshuzaizai.scammony;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fanshuzaizai.
 * @date 2019/5/8 17:13
 */
@Aspect
public class AopConfig implements EnvironmentAware {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AopConfig.class);

    @Autowired
    private ScammonyProperties scammonyProperties;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port:null}")
    private String port;

    @Value("${scammony.global.run:true}")
    private boolean globalRun;

    private static String ip;

    private Environment environment;

    static {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取ip出错", e);
        }
    }

    private ConcurrentHashMap<String, Long> localMark = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, RLock> locks = new ConcurrentHashMap<>();

    private RLock getLock(String key) {
        return locks.compute(key, (k, v) -> v != null ? v : redissonClient.getReadWriteLock(scammonyProperties.getLockKeyPrefix() + key).writeLock());
    }

    @Around("@annotation(com.github.fanshuzaizai.scammony.MethodSwitch) || @within(com.github.fanshuzaizai.scammony.MethodSwitch)")
    public Object annotation(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodSwitch methodSwitch = method.getAnnotation(MethodSwitch.class);
        String fullMethodName = method.getDeclaringClass().getName() + "." + method.getName();

        if (log.isDebugEnabled()) {
            log.debug("{}】准备执行【{}】方法", getVerifyName(), fullMethodName);
        }
        //全局开关关闭
        if (methodSwitch.effectGlobalSetting() && !getGlobalSwitch()) {
            if (log.isDebugEnabled()) {
                log.debug("方法被关闭,本次忽略");
            }
            return null;
        }

        boolean autoCatch = methodSwitch.autoCatch();

        //不是唯一类型
        if (!methodSwitch.sole()) {
            if (log.isDebugEnabled()) {
                log.debug("不是唯一类型，开始执行");
            }
            return execute(pjp, autoCatch);
        }

        String redisLock;
        String code = methodSwitch.code();
        if (StringUtils.hasText(code)) {
            redisLock = code;
        } else {
            redisLock = fullMethodName;
        }

        RLock lock = getLock(redisLock);
        try {
            //1.抢锁
            if (lock.tryLock()) {
                //redis中保存的每个客户端的标记
                Object o = stringRedisTemplate.opsForHash().get(scammonyProperties.getMarkKey(), redisLock);
                //2.对比redis和本地中每个code的value
                if (o == null || Objects.equals(Long.parseLong(o.toString()), localMark.get(redisLock))) {
                    if (log.isDebugEnabled()) {
                        log.debug("抢到锁了，开始执行");
                    }
                    //更新redis和本地的标记
                    Long currentTime = System.currentTimeMillis();
                    stringRedisTemplate.opsForHash().put(scammonyProperties.getMarkKey(), redisLock, currentTime.toString());
                    localMark.put(redisLock, currentTime);
                    //执行任务
                    return execute(pjp, autoCatch);
                } else {
                    //已经被其他服务执行过了，更新本地标记
                    localMark.put(redisLock, Long.parseLong(o.toString()));
                }
            }
            //忽略执行
            if (log.isDebugEnabled()) {
                log.debug("已被其他服务执行，本次忽略");
            }
            return null;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Object execute(ProceedingJoinPoint pjp, boolean autoCatch) throws Throwable {
        if (autoCatch) {
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                log.error("出现异常", throwable);
                throw throwable;
            }
        } else {
            return pjp.proceed();
        }

    }

    private String getVerifyName() {
        return " [ip]：" + ip + " [port]：" + port;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public boolean getGlobalSwitch() {
        String id = scammonyProperties.getGlobal().getId();
        if (!StringUtils.hasText(id)) {
            String idKey = scammonyProperties.getGlobal().getIdKey();
            id = environment.getProperty(idKey);
        }
        return environment.getProperty("scammony.global.setting." + id + ".run", Boolean.class, true);
    }
}
