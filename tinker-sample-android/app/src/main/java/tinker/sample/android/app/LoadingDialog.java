package tinker.sample.android.app;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingDialog {

    private final Context context;
    private AlertDialog dialog;

    public LoadingDialog(Context context) {
        this.context = context;
    }

    /**
     * 显示加载对话框
     * @param message 加载提示文本
     */
    public void showLoading(String message) {
        // 创建一个线性布局容器
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 50, 50, 50); // 设置内边距
        container.setGravity(Gravity.CENTER); // 内容居中

        // 添加进度条
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true); // 设置为不确定状态
        container.addView(progressBar);

        // 添加文本提示
        TextView messageTextView = new TextView(context);
        messageTextView.setText(message);
        messageTextView.setTextSize(16f); // 设置字体大小
        messageTextView.setTextColor(Color.BLACK);
        messageTextView.setPadding(0, 20, 0, 0); // 设置进度条和文字的间距
        container.addView(messageTextView);

        // 构建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(container); // 将容器设置为对话框视图
        builder.setCancelable(false); // 设置不可取消

        dialog = builder.create();

        // 设置对话框背景为透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    /**
     * 隐藏加载对话框
     */
    public void dismissLoading() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
