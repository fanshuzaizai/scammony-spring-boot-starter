package com.github.fanshuzaizai.scammony;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在方法上使用则对该方法有效
 * <p>
 * 在类或接口上使用会对所有方法生效
 *
 * @author fanshuzaizai.
 * @date 2019/4/26 11:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MethodSwitch {

    /**
     * 在多集群下唯一执行
     * 会用全类名+方法名创建一个redis锁
     */
    boolean sole() default true;

    /**
     * 如果不为空，相同code的任务将共用一把锁
     * 优先判断 {@link MethodSwitch#sole()}，如果为false则本属性无效
     * 会用code创建一个redis锁
     * <p>
     * example
     * 3个定时任务在 每秒/每分/每小时 执行，使用相同code会避免同时执行
     * </p>
     */
    String code() default "";

    /**
     * 自动捕获异常
     * <p>
     * 特别适用于定时任务，要么手动捕获，要么异常被吃掉
     * </p>
     */
    boolean autoCatch() default true;

    /**
     * 是否受全局配置的影响
     * {@link ScammonyProperties.Global}
     */
    boolean effectGlobalSetting() default true;
}
