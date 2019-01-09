package group.tonight.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Rect;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;

public class HongBaoAccessibilityService extends AccessibilityService {
    //微信地球启动页
    private static final String LAYOUT_SPLASH_PAGE = "com.tencent.mm.app.WeChatSplashActivity";
    //新版微信第二启动页
    private static final String LAYOUT_WHATS_NEW = "com.tencent.mm.plugin.whatsnew.ui.WhatsNewUI";
    //微信首页
    private static final String LAYOUT_WECHAT_HOME = "com.tencent.mm.ui.mogic.WxViewPager";
    //聊天列表
    private static final String LAYOUT_CHAT_LIST = "com.tencent.mm.ui.LauncherUI";
    //拆红包界面
    private static final String LAYOUT_OPEN_RED_BAG = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";
    //红包详情
    private static final String LAYOUT_RED_BAG_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    //微信更新
    private static final String LAYOUT_UPDATE_WECHAT = "com.tencent.mm.plugin.webview.ui.tools.WebViewUI";

    private String mCurrentActivityName;
    private boolean mOpenningRedBag;
    private List<AccessibilityNodeInfo> mAvailableNodeInfosList = new ArrayList<>();
    private List<AccessibilityNodeInfo> mHistoryClickedNodeList = new ArrayList<>();
    private int mScreenWidthPixels;
    private int mScreenHeightPixels;
    private int mHalfScreenWidth;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mScreenWidthPixels = getResources().getDisplayMetrics().widthPixels;
        mScreenHeightPixels = getResources().getDisplayMetrics().heightPixels;
        mHalfScreenWidth = ((int) (mScreenWidthPixels / 2.0f));
        KLog.e("手机分辨率—>宽：" + mScreenWidthPixels + "，高：" + mScreenHeightPixels);
        KLog.e("半屏宽：" + mHalfScreenWidth);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        CharSequence className = event.getClassName();
        KLog.e("eventType:" + eventType + " 当前窗口名称：" + className);

        if (className != null && className.toString().contains("com.tencent.mm")) {
            mCurrentActivityName = className.toString();
        }
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED://1   视图被点击
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED://2
            case AccessibilityEvent.TYPE_VIEW_SELECTED://4
            case AccessibilityEvent.TYPE_VIEW_FOCUSED://8   视图获得焦点
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED://16
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://32
                if (LAYOUT_CHAT_LIST.equals(mCurrentActivityName)) {
                    KLog.e("微信首页打开");
                    mOpenningRedBag = false;
                } else if (LAYOUT_OPEN_RED_BAG.equals(mCurrentActivityName)) {
                    KLog.e("红包弹窗打开");
                    //com.tencent.mm:id/cv0(android.widget.Button)->com.tencent.mm:id/cuv(android.widget.RelativeLayout)->
                    //如果有拆红包圆形按钮，不需要关闭弹窗
                    List<AccessibilityNodeInfo> openButtonNodeInfoList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cv0");//开
                    AccessibilityNodeInfo openNode = null;//拆红包按钮
                    for (AccessibilityNodeInfo nodeInfo : openButtonNodeInfoList) {
                        if ("android.widget.Button".equals(nodeInfo.getClassName().toString())) {
                            if (nodeInfo.isClickable()) {
                                openNode = nodeInfo;
                                break;
                            }
                        }
                    }
                    if (openNode != null) {
                        KLog.e("红包未被拆开，自动拆开红包");
                        openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    } else {
                        KLog.e("红包显示未拆，但实际被拆开了，自动关闭红包弹窗");
                        rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cs9").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                } else if (LAYOUT_RED_BAG_DETAIL.equals(mCurrentActivityName)) {
                    KLog.e("红包详情打开");
                    mOpenningRedBag = false;

                    if (getRootInActiveWindow() != null) {
                        List<AccessibilityNodeInfo> moneyNodeInfo = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cqv");//金额
                        if (moneyNodeInfo != null) {
                            if (!moneyNodeInfo.isEmpty()) {
                                CharSequence text = moneyNodeInfo.get(0).getText();
                                String tips = "抢到" + text + "元";
                                KLog.e(tips);
                                Toast.makeText(this, tips, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    // 红包详情左上角返回按钮
                    for (AccessibilityNodeInfo nodeInfo : rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/k4")) {//返回
                        if (nodeInfo.isClickable()) {
                            //手动返回
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://64    通知栏状态改变
                //模拟打开消息通知栏（锁屏状态下无法自动抢红包）
                //https://blog.csdn.net/qq_24531461/article/details/53884538
                //当微信位于后台时，通知栏来红包消息时，自动打开红包消息，并执行抢红包操作
                List<CharSequence> textList = event.getText();
                for (CharSequence charSequence : textList) {
                    if (!charSequence.toString().contains("[微信红包]")) {
                        continue;
                    }
                    Parcelable parcelableData = event.getParcelableData();
                    if (parcelableData == null) {
                        continue;
                    }
                    if (!(parcelableData instanceof Notification)) {
                        continue;
                    }
                    Notification notification = (Notification) parcelableData;
                    PendingIntent contentIntent = notification.contentIntent;
                    try {
                        //模拟点击通知栏消息
                        contentIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                        KLog.e(e.getMessage());
                    }
                    break;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER://128
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT://256
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START://512
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END://1024
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://2048  窗口内容改变
                if (mCurrentActivityName != null) {
                    if (LAYOUT_CHAT_LIST.equals(mCurrentActivityName)) {
                        if (mOpenningRedBag) {
                            return;
                        }
                        if (hasRedBag()) {
                            List<AccessibilityNodeInfo> nodeInfosByViewId = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ao4");//点开红包弹窗
                            if (!nodeInfosByViewId.isEmpty()) {
                                mOpenningRedBag = true;
                                KLog.e("当前窗口未拆的红包数量：" + mAvailableNodeInfosList.size());
                                if (!mAvailableNodeInfosList.isEmpty()) {
                                    // TODO: 2019/1/9 0009 自己发3个红包，其中一个好友抢一个，自己抢一个，剩下一个，
                                    // TODO: 2019/1/9 0009 红包消息中的红包还没领完，所以不会出现已领完，那这里抢完后回到消息窗口又会自动抢剩下的。。。
                                    // TODO: 2019/1/9 0009 这里是个bug
                                    // TODO: 2019/1/9 0009 也就是说自己发两个以上的红包自己抢会有bug
                                    //不抢自己发的红包就可以简单解决自己抢自己红包反复弹窗的bug
                                    KLog.e(mAvailableNodeInfosList.toString());
                                    AccessibilityNodeInfo nodeInfo = mAvailableNodeInfosList.remove(0);
                                    if (!mHistoryClickedNodeList.contains(nodeInfo)) {
                                        mHistoryClickedNodeList.add(nodeInfo);
                                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    } else {
                                        KLog.e("该多人红包已领取过：" + nodeInfo);
                                    }
                                }
                            }
                        }
                    } else if (LAYOUT_OPEN_RED_BAG.equals(mCurrentActivityName)) {
                    } else if (LAYOUT_RED_BAG_DETAIL.equals(mCurrentActivityName)) {
                    }
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED://4096   视图滚动
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED://                  8192
            case AccessibilityEvent.TYPE_ANNOUNCEMENT://                                16384
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED://                  32768
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED://            65536
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY://131072
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START://                    262144
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END://                      524288
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START://                   1048576
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END://                     2097152
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED://                           4194304
            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED://                      8388608
            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT://                   16777216
            default:
                break;
        }

    }

    private Rect mTempRect = new Rect();

    private boolean hasRedBag() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow == null) {
            return false;
        }
        //每个聊天联系人item
        //红包布局，包含红包图标、恭喜发财、已被领完
        //所有可点击的红包，包括已拆与未拆，还有纯图片消息
        //筛选出仅包含“微信红包”标签的消息
        List<AccessibilityNodeInfo> redBagNodeList = new ArrayList<>();
        for (AccessibilityNodeInfo nodeInfo : rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ao4")) {
            List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/apf");//白色微信红包标签
            if (nodeInfos != null) {
                if (!nodeInfos.isEmpty()) {
                    redBagNodeList.add(nodeInfo);
                }
            }
        }
        if (redBagNodeList.isEmpty()) {
            KLog.e("当前聊天窗口中没有红包消息");
            return false;
        }
        KLog.e("发现红包消息个数：" + redBagNodeList.size());
        //从最新消息往旧消息查找红包，查到未拆红包，立刻跳出循环
        mAvailableNodeInfosList.clear();
        for (int i = redBagNodeList.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo nodeInfo = redBagNodeList.get(i);//所有可点击的红包结点
            boolean isSelfRedBag = false;
            if (nodeInfo.getParent() != null) {
                List<AccessibilityNodeInfo> infoList = nodeInfo.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/nj");//头像
                if (infoList != null) {
                    if (!infoList.isEmpty()) {
                        infoList.get(0).getBoundsInScreen(mTempRect);
                        System.out.println(mTempRect);
                        if (mTempRect.left > mHalfScreenWidth) {
                            isSelfRedBag = true;
                        }
                    }
                }
            }
            List<AccessibilityNodeInfo> infoList = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ape");//看有没有“已被领完”结点的id存在
            if (infoList != null) {
                if (infoList.isEmpty()) {
                    KLog.e("有红包未拆：" + i);
                    if (nodeInfo.isClickable()) {
                        if (!isSelfRedBag) {//仅检测不是自己发的红包
                            mAvailableNodeInfosList.add(nodeInfo);
                        } else {
                            KLog.e("自己发的红包需要手动抢，自动抢有问题，待解决");
                        }
                    } else {
                        throw new IllegalStateException("红包结点不可点击");
                    }
                } else {
                    if (TextUtils.equals(infoList.get(0).getText(), "已被领完")) {
                        KLog.e("当前红包已被拆开了：" + i);
                    }
                }
            }
        }
        if (mAvailableNodeInfosList.isEmpty()) {
            KLog.e("窗口中所有红包均已被拆开");
            return false;
        }
        KLog.e("窗口中有未领的红包");
        return true;
    }

    @Override
    public void onInterrupt() {

    }
}
