### 关于这个项目：

这个项目兼容的系统的SharedPreferences，只是更改了系统SharedPreferences的Context.MODE_MULTI_PROCESS的mode的实现，
使用了ContentProvider配合Context.MODE_PRIVATE的SharedPreferences来进行跨进程存取数据，
顺便改掉了Context.MODE_MULTI_PROCESS的mode的registerOnSharedPreferenceChangeListener的实现，
以避免注册回调后可能会不执行回调的问题。

更加详细描述请参考：<a href="https://caijun-carr.github.io/blog/2018/07/11/multi-process-shared-prefrences/" target="_blank">我的博客</a>

ps：使用了registerOnSharedPreferenceChangeListener一定要记得使用对应的unregisterOnSharedPreferenceChangeListener，否则会造成内存泄露！



### About this project:

This project is compatible with ContextImpl's SharedPreferences, just changing the implementation of ContextImpl's SharedPreferences Context.MODE_MULTI_PROCESS mode.
Use ContentProvider with Context.MODE_PRIVATE's SharedPreferences to access data across processes.
By the way, the implementation of the registerOnSharedPreferenceChangeListener of the mode of Context.MODE_MULTI_PROCESS is changed.
To avoid the problem of callbacks being executed after the callback is registered.

For a more detailed description, please refer to: <a href="https://caijun-carr.github.io/blog/2018/07/11/multi-process-shared-prefrences/" target="_blank">My blog</a>

ps:Use registerOnSharedPreferenceChangeListener must remember to use the corresponding unregisterOnSharedPreferenceChangeListener, otherwise it will cause memory leaks!