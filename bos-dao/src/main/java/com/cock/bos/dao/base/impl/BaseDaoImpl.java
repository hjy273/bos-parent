package com.cock.bos.dao.base.impl;

import com.cock.bos.dao.base.IBaseDao;
import com.cock.bos.utils.PageBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.query.Query;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 持久层通用实现
 *
 * @param <T>
 */
public class BaseDaoImpl<T> extends HibernateDaoSupport implements IBaseDao<T> {

    //某个实体的类型
    private Class<T> entityClass;

    //根据类型注入spring工厂中的会话工厂对象sessionFactory
    @Resource
    public void setMySessionFactory(SessionFactory sessionFactory) {
        super.setSessionFactory(sessionFactory);
    }

    //在父类（BaseDaoImpl）的构造方法中动态获得entityClass
    public BaseDaoImpl() {
        ParameterizedType superclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        //获得父类上声明的泛型数组
        Type[] actualTypeArguments = superclass.getActualTypeArguments();
        entityClass = (Class<T>) actualTypeArguments[0];
    }

    @Override
    public void save(T entity) {
        this.getHibernateTemplate().save(entity);
    }

    @Override
    public void delete(T entity) {
        this.getHibernateTemplate().delete(entity);
    }

    @Override
    public void update(T entity) {
        this.getHibernateTemplate().update(entity);
    }

    @Override
    public T findById(Serializable id) {
        return this.getHibernateTemplate().get(entityClass, id);
    }

    @Override
    public List<T> findAll() {
        String hql = "FROM " + entityClass.getSimpleName();
        return (List<T>) this.getHibernateTemplate().find(hql);
    }

    /**
     * 执行更新
     *
     * @param queryName
     * @param objects
     */
    @Override
    public void executeUpdate(String queryName, Object... objects) {
        Session session = this.getSessionFactory().getCurrentSession();
        Query query = session.getNamedQuery(queryName);
        int i = 0;
        for (Object object : objects) {
            //为HQL语句中的？赋值
            query.setParameter(i++, object);
        }
        //执行更新
        query.executeUpdate();
    }

    /**
     * 通用分页查询方法
     *
     * @param pageBean
     */
    @Override
    public void pageQuery(PageBean pageBean) {
        int currentPage = pageBean.getCurrentPage();
        int pageSize = pageBean.getPageSize();
        DetachedCriteria detachedCriteria = pageBean.getDetachedCriteria();
        //查询total---总数据量
        detachedCriteria.setProjection(Projections.rowCount());
        //指定hibernate框架发出sql的形式----》select count(*) from bc_staff;
        List<Long> countList = (List<Long>) this.getHibernateTemplate().findByCriteria(detachedCriteria);
        Long count = countList.get(0);
        pageBean.setTotal(count.intValue());

        //查询rows---当前页需要展示的数据集合
        //指定hibernate框架发出sql的形式----》select * from bc_staff;
        detachedCriteria.setProjection(null);
        int firstResult = (currentPage - 1) * pageSize;
        int maxResult = pageSize;
        List rows = this.getHibernateTemplate().findByCriteria(detachedCriteria, firstResult, maxResult);
        pageBean.setRows(rows);
    }

}
