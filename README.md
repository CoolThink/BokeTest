# BokeTest
博客地址:http://blog.csdn.net/ys408973279/article/details/50403756


这篇文章主要配套与[Android内存优化之——static使用篇](http://blog.csdn.net/ys408973279/article/details/50389200)向大家介绍MAT工具的使用，我们分析的内存泄漏程序是上一篇文章中static的使用内存泄漏的比较不容易发现泄漏的第二情况和第三种情况——不正确使用单例和asyncTask造成的内存泄漏现象，没看上一篇文章的大家可以先阅读下上一篇文章。
先看一下我们需要分析的目标程序由3个activity组成：

**MainActivity.java**
```
public class MainActivity extends AppCompatActivity {

    private Button mNextButton;
    private TextView pageTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        pageTextView= (TextView) findViewById(R.id.tv_page);
        pageTextView.setText("MainActivity");
        mNextButton= (Button) findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,SigleLeakActivity.class);
                startActivity(intent);
            }
        });
    }
}
```
这是一个正常的activity主要是用来启动后面的activity页面。

**SigleLeakActivity.java**
```
public class SigleLeakActivity extends AppCompatActivity{

    private MyListener mMyListener=new MyListener() {
        @Override
        public void onSomeThingHappen() {
        }
    };
    private TestManager testManager=TestManager.getInstance();
    private Button mNextButton;
    private TextView pageTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        pageTextView= (TextView) findViewById(R.id.tv_page);
        pageTextView.setText("SigleLeakActivity");
        mNextButton= (Button) findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SigleLeakActivity.this,AysncTaskLeakActivity.class);
                startActivity(intent);
            }
        });
        testManager.registerListener(mMyListener);
    }

}
```
**TestManager 单例**
```
public class TestManager {
    public static final TestManager INSTANCE = new TestManager();
    private List<MyListener> mListenerList;

    private TestManager() {
        mListenerList = new ArrayList<MyListener>();
    }

    public static TestManager getInstance() {
        return INSTANCE;
    }

    public void registerListener(MyListener listener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener);
        }
    }
    public void unregisterListener(MyListener listener) {
        mListenerList.remove(listener);
    }
}

interface MyListener {
    public void onSomeThingHappen();
}
```
在SigleLeakActivity里，由于对单例的不正确使用会造成内存泄漏

**AysncTaskLeakActivity.java**
```
public class AysncTaskLeakActivity extends AppCompatActivity {
    AsyncTask mTask;
    private Button mNextButton;
    private TextView pageTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        pageTextView= (TextView) findViewById(R.id.tv_page);
        pageTextView.setText("AysncTaskLeakActivity");
        mNextButton= (Button) findViewById(R.id.btn_next);
        mTask=new AsyncTask<String,Void,Void>()
        {
            @Override
            protected Void doInBackground(String... params) {
                //doSomething..
                Boolean loop=true;
                while (loop) {
                    Log.d("test","task is running");
                }
                return null;
            }
        }.execute("a task");
    }

}
```

**回顾下内存泄漏的原因：**

**1.SigleLeakActivity**
在SigleLeakActivity中，非静态的内部类的对象都是会持有指向外部类对象的引用的，因此我们将内部类对象mMyListener让单例所持有时，由于mMyListener引用了我们的activity对象，因此造成activity对象也不能被回收了，从而出现内存泄漏现象。
**2.AysncTaskLeakActivity**
我们的内部类的实例mTask会持有对activity实例对象的引用了。查看AsyncTask的实现，会通过一个SerialExecutor串行线程池来对我们的任务进行排队，而这个SerialExecutor对象就是一个static final的常量。 
具体的引用关系是: 
1.我们的任务被封装在一个FutureTask的对象中(它充当一个runable的作用)，FutureTask的实现也是通过内部类来实现的，因此它也为持有AsyncTask对象，而AsyncTask对象引用了activity对象，因此activity对象间接的被FutureTask对象给引用了。 
2.futuretask对象会被添加到一个ArrayDeque类型的任务队列的mTasks实例中 
3.mTasks任务队列又被SerialExecutor对象所持有，刚也说了这个SerialExecutor对象是一个static final的常量。 
    具体AsyncTask的实现大家可以去参照下其源代码，我这里就通过文字描述一下其添加任务的实现过程就可以了，总之分析了这么多通过层层引用后我们的activity会被一个static变量所引用到。

在接下来的MAT工具的分析中我们将可以更加直观的看到造成这些内存泄漏的这层层引用关系。


最后在看下布局文件:
```
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingBottom="@dimen/activity_vertical_margin"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    tools:context="com.thinkcool.boketest.TestActivity">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center">
        <ImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="32dp"
            android:src="@mipmap/ic_launcher"/>
        <TextView
            android:id="@+id/tv_page"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="24sp"
            android:text="1" />
    </LinearLayout>
    <Button
        android:id="@+id/btn_next"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="跳转"/>
</FrameLayout>
```
   布局非常简单我们放置了一个TextView用于显示当前activity的名称。一个Button用于跳转下一个activity。
   在mainActivity里跳转的是SigleLeakActivity，而在SigleLeakActivity点击button跳转到AysncTaskLeakActivity。AysncTaskLeakActivity里就没有跳转了。
   
   **下面我们进行如下操作:**
   1.点击"跳转"这个button 2次，此时我们的任务栈里将会有3个Activity的实例:mainActivity、SigleLeakActivity、AysncTaskLeakActivity。
   2.然后我们按返回键2次，这样又回到了刚启动的是情况了，此时任务栈只有1个mainActivity页面。
   按照正常的情况来说，返回了的那2个activity都应该被回收掉才对，但是按照我们之前内存泄漏的分析，这2个activity由于被static变量所引用并不会被回收。
   
**下面我们就使用Memory Analyzer工具来验证下是不是这样吧：**
使用MAT(Memory Analyzer)分析内存泄漏，当然我们得下载Memory Analyzer工具(解压即可用):http://www.eclipse.org/mat/downloads.php

首先我们在Android studio中或eclipse的DDMS里导出我们程序的hprof文件。

![这里写图片描述](http://img.blog.csdn.net/20151225173417403)

找到我们的目标程序点击dump hprof file按钮即可。

然后使用android sdk提供的hprof-conv工具将hprof文件转换为MAT能识别的hprof文件。
![这里写图片描述](http://img.blog.csdn.net/20151225174303774) 

即在android sdk platform-tools目录下执行: 
`hprof-conv.exe filename.hprof  filename-conv.hprof`

然后打开MAT,打开filename-conv.hprof文件:
![这里写图片描述](http://img.blog.csdn.net/20151225184829997)

可以看到MAT提供了很多功能，如:Histogram:可以直观的看到不同类型的buffer的数量和占用的内存大小，Dominator Tree:则把内存中对象按照从大到小进行了排序。其中我们还可以使用OQL对我们想要关心的object进行查找功能，在这个例子里我们主要分析的时候静态变量对activity对象的引用所造成的内存泄漏现象，因此我们可以进行如下操作：
![这里写图片描述](http://img.blog.csdn.net/20151225232814101)
打开OQL页面，然后输入`select * from instanceof android.app.Activity`查询条件，然后点击红色感叹号执行。
这里我们可以看到如我们分析的一样虽然任务栈里只有了一个activity对象，但另外那2个testActivity对象仍然没有被释放。

进一步分析：对那两个activity分别进行如下操作，右键->Path To GC Root->exclude wake/soft refrence(这里排除了弱引用和软引用，因为两者被gc回收的几率较大)。

**分析SigleLeakActivity：**

![这里写图片描述](http://img.blog.csdn.net/20151225233025261)

**分析AysncTaskLeakActivity:**

![这里写图片描述](http://img.blog.csdn.net/20151225233337917)

看到到这两张图，就显得非常明了了，对于之前分析的在SigleLeakActivity中,和AysncTaskLeakActivity中，activity和static变量的层层引用关系都显示在分析图上了。
1.SigleLeakActivity（发现最终的引用就是INSTANCE这个静态常量）
2.AysncTaskLeakActivity（发现最终的引用就是serial_exector这个静态常量）
这样我们根据这个层级关系就可以定位到内存泄漏的位置就是在对testManager这个单例和对asyncTask的使用这里了。再仔细观察代码，修改后就能做到对内存使用的优化了。

**修改优化后的代码:**

SigleLeakActivity中:
```
 @Override
    protected void onDestroy() {
        testManager.unregisterListener(mMyListener);
        super.onDestroy();
    }
```
AysncTaskLeakActivity中:

```
 ....
                Boolean loop=true;
                while (loop) {
                    if(isCancelled()) {
                      Log.d("test","task exit");
                    return null;
              }
                    Log.d("test","task is running");
                }
                return null;
 ....
                 @Override
    protected void onDestroy() {
        mTask.cancel(true);
        super.onDestroy();
    }

```
然后按照之前的步骤导出hprof文件用MAT再次分析:

![这里写图片描述](http://img.blog.csdn.net/20151225235255408)

此时就没有了内存泄漏的现象了。

对于MAT的使用(用于排除static对activity的引用而造成的泄漏问题)就介绍到这儿，最后将代码上传至github做个标记(https://github.com/CoolThink/BokeTest.git)。

喜欢文章的欢迎关注点赞！~
