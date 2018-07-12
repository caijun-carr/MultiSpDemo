### 系统的sp跨进程的问题

在讨论这些问题之前先要了解系统sp的一些特点。

系统的sp是自带了一个缓存的机制的，在ContextImpl里有个static的的ArrayMap保存了获取过的SharedPreferences对象，在写入和读取的时候会先从这个缓存里读取SharedPreferences对象进行操作。另外SharedPreferences的registerOnSharedPreferenceChangeListener方法注册变更回调的时候，存储的回调是用的WeakHashMap保存的，在GC回收以后，会把这个注册过的回调清空导致有变更的时候不会执行回调。

**1.内容更新不及时**

当一个进程改完之后另外的进程无法及时知道，但是系统的sp有考虑到一点这个问题，从源码ContextImpl.getSharedPreferences方法里可以看出来
```java
@Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferencesImpl sp;
        synchronized (ContextImpl.class) {

          ···

          if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
            getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
            // If somebody else (some other process) changed the prefs
            // file behind our back, we reload it.  This has been the
            // historical (if undocumented) behavior.
            sp.startReloadIfChangedUnexpectedly();
        }
        return sp;
    }

```
如果mode为Context.MODE_MULTI_PROCESS会从文件里重新读取数据。所以在担心一个进程在更改某个值以后另外的进程不能及时获取到更新是比较好解决的。

比如项目之前的处理方法是，在另外的进程里定义一个service，然后定义一个Action，在更新了跨进程的sp的某个值的时候，start那个service带上值的的Action，然后service的onStartCommand方法里当接收到对应的Action时再去获取一遍跨进程的SharedPreferences。

**2.多个进程同时写数据，会造成数据丢失**

之前项目里也有小伙伴因为用错SharedPreferences，而导致我在主进程的SharedPreferences数据丢失的问题。猜测是因为这个原因导致了，排查完发现真的是这个原因，改用一个另外的SharedPreferences解决之。

这个问题貌似除了通过AIDL等跨进程的方式，协同在一个进程里操作好像也没有很好的解决方法。不过如果是普通的类似设置类的操作，在其他的进程几乎只读取不写入的方式几乎不会影响到。

**3.变更无法及时收到回调**

上面分析了registerOnSharedPreferenceChangeListener方法注册变更回调的时候，存储的回调是用的WeakHashMap保存的，在GC回收以后，会把这个注册过的回调清空导致有变更的时候不会执行回调。

但是这只是在同一个进程存在的问题，不同的进程注册这个就算回调没有被回收掉也同样收不到变更的回调。系统的SharedPreferences就没有通知过其他进程。

### 解决上面这些问题

**1.解决跨进程的问题**

解决跨进程的问题，最容易想到的是通过ContentProvider来跨进程存储数据。
毫无疑问，ContentProvider用来解决跨进程的问题是毫无问题的，因为ContentProvider原本就是提供这个功能的。但是一般情况下ContentProvider都是配合数据库用的，似乎有一种用宰牛刀啥鸡的味道，就抛出了下面的问题。

**2.用数据库太繁重的问题**

上面说道了只是用来存储SharedPreferences而去创建一个专用的数据库似乎太重操作起来也比较麻烦。还可能会导致第一次存储的时候初始化时间比较长的问题（因为还要创建数据库之类的）。

而通过ContentProvider来跨进程存储数据仍然用SharedPrefrences呢？就相当于其他进程的数据只是通过ContentProvider来告知某个单独的进程，读取和存储都是只在这个进程里进行，而只是通过ContentProvider起到一个传递的作用。这样不就既解决了多个进程写入数据造成丢失的问题又解决了数据库太重的问题？

**3.跨进程数据变更的回调**

通过注册ContentProvider的registerContentObserver，指定uri的值在ContentProvider中调用getContext().getContentResolver().notifyChange(uri, null);就能解决跨进程回调的问题了。同时还自己定义了registerOnSharedPreferenceChangeListener，可以使用强引用来解决回调可能被回收的问题，当然必须要及时调用unregisterOnSharedPreferenceChangeListener方法，不然可能会造成内存泄露。