package group.tonight.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;

public class HongBaoAccessibilityService extends AccessibilityService {
    //聊天列表
    private static final String LAYOUT_CHAT_LIST = "com.tencent.mm.ui.LauncherUI";
    //拆红包界面
    private static final String LAYOUT_OPEN_RED_BAG = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";
    //红包详情
    private static final String LAYOUT_RED_BAG_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    private String mCurrentActivityName;
    private boolean mOpenningRedBag;
    private List<AccessibilityNodeInfo> mAvailableNodeInfosList = new ArrayList<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        CharSequence className = event.getClassName();
        if (className != null && className.toString().contains("com.tencent.mm")) {
            mCurrentActivityName = className.toString();
        }
        KLog.e("eventType:" + eventType + " 当前窗口名称：" + mCurrentActivityName + " 红包是否正在打开：" + mOpenningRedBag);

        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED://1
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED://2
            case AccessibilityEvent.TYPE_VIEW_SELECTED://4
            case AccessibilityEvent.TYPE_VIEW_FOCUSED://8
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED://16
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://32
                if (LAYOUT_CHAT_LIST.equals(mCurrentActivityName)) {
                    KLog.e("微信首页打开");
                    mOpenningRedBag = false;
                } else if (LAYOUT_OPEN_RED_BAG.equals(mCurrentActivityName)) {
                    KLog.e("红包弹窗打开");
                    if (mAvailableNodeInfosList.isEmpty()) {
                        if (closeOpenBagDialog()) {
                            mOpenningRedBag = false;
                        }
                    }
                } else if (LAYOUT_RED_BAG_DETAIL.equals(mCurrentActivityName)) {
                    KLog.e("红包详情打开");
                    mOpenningRedBag = false;
                }
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://64
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER://128
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT://256
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START://512
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END://1024
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://2048
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
                                    mAvailableNodeInfosList.remove(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                }
                            }
                        }
                    } else if (LAYOUT_OPEN_RED_BAG.equals(mCurrentActivityName)) {
                        if (closeOpenBagDialog()) {
                            return;
                        }
                        for (final AccessibilityNodeInfo nodeInfo : rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cv0")) {//拆开红包
                            if (nodeInfo.isClickable() && nodeInfo.getClassName().equals("android.widget.Button")) {
                                KLog.e("onAccessibilityEvent: 拆开红包");
                                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                break;
                            }
                        }
                    } else if (LAYOUT_RED_BAG_DETAIL.equals(mCurrentActivityName)) {
                        // 红包详情左上角返回按钮
                        for (AccessibilityNodeInfo nodeInfo : rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/k4")) {//返回
                            if (nodeInfo.isClickable()) {
                                //手动返回
                                break;
                            }
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED://4096
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED://8192
            case AccessibilityEvent.TYPE_ANNOUNCEMENT://16384
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED://32768
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED://65536
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY://131072
            default:
                break;
        }

    }

    private boolean closeOpenBagDialog() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow == null) {
            return true;
        }
        //android.widget.TextView
//        List<AccessibilityNodeInfo> noBagNodeList = rootInActiveWindow.findAccessibilityNodeInfosByText("手慢了，红包派完了");
        List<AccessibilityNodeInfo> noBagNodeList = rootInActiveWindow.findAccessibilityNodeInfosByText("看看大家的手气");
        if (noBagNodeList.isEmpty()) {
            KLog.e("红包未被拆开，自动拆开红包");
            return false;
        }
        KLog.e("红包显示未拆，但实际被拆开了，自动关闭红包弹窗");
        rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cs9").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return true;
    }

    private boolean hasRedBag() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow == null) {
            return false;
        }
        //每个聊天联系人item
        //红包布局，包含红包图标、恭喜发财、已被领完
        //所有可点击的红包，包括已拆与未拆
        List<AccessibilityNodeInfo> nodeInfoList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ao4");
        //如果当前聊天窗口中没有红包消息，走这里
        if (nodeInfoList.isEmpty()) {
            KLog.e("当前聊天窗口中没有红包消息");
            return false;
        }
        //从最新消息往旧消息查找红包，查到未拆红包，立刻跳出循环
        mAvailableNodeInfosList.clear();
        for (int i = nodeInfoList.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo nodeInfo = nodeInfoList.get(i);//所有可点击的红包结点
            List<AccessibilityNodeInfo> infoList = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ape");//看有没有“已被领完”结点的id存在
            if (infoList != null) {
                if (infoList.isEmpty()) {
                    KLog.e("有红包未拆：" + i);
                    if (nodeInfo.isClickable()) {
                        mAvailableNodeInfosList.add(nodeInfo);
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
