package cn.cccs7.cynexus.common.core.mapper;

import java.util.List;

public interface BaseMapper<S, T> {

    T toTarget(S source);

    S toSource(T target);

    List<T> toTargetList(List<S> sourceList);

    List<S> toSourceList(List<T> targetList);
}
