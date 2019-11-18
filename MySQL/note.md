# 一些说明

``start transaction;``
``commit;``
``rollback;``
``SHOW VARIABLES LIKE 'autocommit';``
``savepoint name``(保存点的名称)
``rollback to name`` 回滚到该保存点
在``mysql``中，重复插入的数据会被保留
读：``select``
写:``delete update insert``这三种操作均叫做写
并发事务访问相同记录的情况大致可以分为三种：

平常所用到的写操作无非是``DELETE、UPDATE、INSERT``

1. 写-写：即并发事务相继对相同的记录进行写访问（其中写访问包括insert delete update），会出现脏写的问题-通过锁来实现

2. 读-读：并发事务相继对相同的记录进行读操作，这是完全可以接受的
3. 读-写：即并发事务的一个事务读一个事务写，可能先读后写也可能先写后读，会出现脏读，不可重复读，幻读的问题-解决方法：1.通过MVCC，2.通过锁来实现

    + 在``repeatable read``的情况下：通过``MVCC``的``ReadView``，可以解决不可重复读与幻读的情况发生。但是``SQL``标准规定：``repeatable read``仅仅保证不会出现不可重复度就可以了``MySQL``的``InnoDB``引擎默认的RR级别已经通过``MVCC``自动帮我们解决了幻读的问题（不可重复读的问题是肯定已经解决了），但是在插入相同的数据的时候，还是会报错的。并且还可以更新这个不存在的数据，即严格来说对于仅仅进行select操作，是不会出现幻读的现象发生的，但是对于``update`` 和``delete insert`` 还是能操作这个记录的。

    + 记录的增加是属于幻读 记录的减少是属于幻读还是不可重复读（不管是属于幻读还是不可重复读，在``RR``的隔离级别下，均解决了），这个有待商榷 小册上说的和网上的一些有点不一样。

    + 不可重复读：侧重于``update,delete``
    + 幻读：侧重于``insert delete`` 关键是这个``delete``到底是属于不可重复读还是幻读呢？
4. 锁的类型：
    + ``S``锁，``X``锁，
        ``IS``锁，``IX``锁：这两个是意向锁。
        **用法**：当我们对使用``InnoDB``存储引擎的表的**某些记录**加``S``锁之前，那么需要在**表级别**增加一个``IS``锁
        用法：当我们对使用``InnoDB``存储引擎的表的**某些记录**加``X``锁之前，那么需要在**表级别**增加一个``IX``锁
        **作用**：``IS``锁和``IX``锁的使命只是为了后续在加**表级别**的``S``锁和``X``锁时判断表中是否有已经被加锁的记录，以避免用遍历的方式来查看表中有没有上锁的记录。因为只要表中的数据只要有一个是加了``X``锁的，那么这个其他事务就不能对这个表加上``X``锁，因为只要是一个事务对一个表增加了那么大粒度的表级锁，说明对整个表的记录都是独占的，而之前的记录已经被别的事务独占了，从而前后发生了矛盾。
    + 表锁类型：
        1. ``S``锁 ``X``锁 基本没用
        2. ``auto——increment``锁

    + 行锁类型：
        一般情况下，新插入一条记录的操作并不加锁，设计``InnoDB``的大叔通过一种称之为隐式锁的东东来保护这条新插入的记录在本事务提交前不被别的事务访问
        在为``AUTO_INCREMENT``列进行自动递增的时候，为了保证插入的数值整个范围内是递增的，会在表级别进行加锁操作。主要有那种加锁操作：

        1. ``AUTO_INC``锁的作用范围只是单个插入语句，即单个``insert``语句。插入语句执行完成后，这个锁就被释放了，而之前的锁只有在事务结束之后会被释放掉。
        2. 轻量级别的锁

5. 如何解决幻读的出现：
    1. ``MVCC``
    2. 进行加``Gap Lock``锁
    说明：``gap``锁仅仅是为了解决幻读的问题产生的。虽然有共享``gap``锁和独占``gap``锁这样的说法，但是它们起到的作用都是相同的。而且如果你对一条记录加了``gap``锁（不论是共享``gap``锁还是独占``gap``锁），并不会限制其他事务对这条记录加正经记录锁或者继续加``gap``锁，再强调一遍，``gap``锁的作用仅仅是为了防止插入幻影记录的而已。

    3. ``next-key``锁的本质就是一个正经记录锁和一个``gap``锁的合体，它既能保护该条记录，又能阻止别的事务将新记录插入被保护记录前边的间隙。