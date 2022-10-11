package com.odoo.addons.sale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.odoo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cracker
 * Created on 10/8/22.
 */


public class BottomSheetNew extends RelativeLayout {
    public static final String TAG = com.odoo.widgets.bottomsheet.BottomSheet.class.getSimpleName();
    private Context mContext;
    private Builder mBuilder;
    private Menu mMenu;
    private Boolean mShowing = Boolean.valueOf(false);
    private Boolean mIsDismissing = Boolean.valueOf(false);
    private BottomSheetListenersNew.OnSheetItemClickListener mItemListener = null;
    private BottomSheetListenersNew.OnSheetMenuCreateListener mOnSheetMenuCreateListener = null;

    public BottomSheetNew(Context context) {
        super(context);
        this.mContext = context;
    }

    @SuppressLint("RestrictedApi")
    public BottomSheetNew setBuilder(Builder builder) {
        this.mBuilder = builder;
        this.mMenu = new MenuBuilder(this.mContext);
        MenuInflater inflator = ((Activity)this.mContext).getMenuInflater();
        inflator.inflate(this.mBuilder.getSheetMenu(), this.mMenu);
        if(this.mBuilder.getOnSheetMenuCreateListener() != null) {
            this.mOnSheetMenuCreateListener = this.mBuilder.getOnSheetMenuCreateListener();
            this.mOnSheetMenuCreateListener.onSheetMenuCreate(this.mMenu, this.mBuilder.getExtraData());
        }

        this.mItemListener = this.mBuilder.getSheetItemListener();
        return this;
    }

    public boolean isShowing() {
        return this.mShowing.booleanValue();
    }

    public void show() {
        this.init(this.mContext);
    }

    public void dismiss() {
        if(!this.mIsDismissing.booleanValue()) {
            Animation slideOut = AnimationUtils.loadAnimation(this.getContext(), com.odoo.widgets.bottomsheet.R.anim.sheet_out);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    BottomSheetNew.this.mIsDismissing = Boolean.valueOf(true);
                }

                public void onAnimationEnd(Animation animation) {
                    BottomSheetNew.this.post(new Runnable() {
                        public void run() {
                            BottomSheetNew.this.finish();
                        }
                    });
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
            this.startAnimation(slideOut);
        }
    }

    private void finish() {
        this.clearAnimation();
        ViewGroup parent = (ViewGroup)this.getParent();
        if(parent != null) {
            parent.removeView(this);
            parent.removeView(parent.findViewById(com.odoo.widgets.bottomsheet.R.id.sheet_overlay));
        }

        this.mShowing = Boolean.valueOf(false);
        this.mIsDismissing = Boolean.valueOf(false);
    }

    private void prepareMenus(RelativeLayout layout) {
        LinearLayout sheet_view = (LinearLayout)layout.findViewById(com.odoo.widgets.bottomsheet.R.id.sheet_rows);
        sheet_view.removeAllViews();
        List<MenuItem> menuItemList = new ArrayList();

        int menus;
        for(menus = 0; menus < this.mMenu.size(); ++menus) {
            MenuItem item = this.mMenu.getItem(menus);
            if(item.isVisible()) {
                menuItemList.add(item);
            }
        }

        menus = menuItemList.size();
        int columns = 3;
        int rows = menus <= columns?1:(int) Math.floor((double)(menus / columns)) + (menus % columns != 0?1:0);
        int index = 0;

        for(int i = 0; i < rows; ++i) {
            LinearLayout row_view = (LinearLayout) LayoutInflater.from(this.mContext).inflate(R.layout.sheet_row, sheet_view, false);

            for(int j = 0; j < 3; ++j) {
                if(index < menuItemList.size()) {
                    if(((MenuItem)menuItemList.get(j)).isVisible()) {
                        row_view.addView(this.getMenuView((MenuItem)menuItemList.get(index), row_view));
                    } else {
                        row_view.addView(this.getDummyMenuView(row_view));
                    }
                } else {
                    row_view.addView(this.getDummyMenuView(row_view));
                }

                ++index;
            }

            sheet_view.addView(row_view);
        }

    }

    @SuppressLint("WrongConstant")
    private View getDummyMenuView(ViewGroup parent) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(this.mContext).inflate(com.odoo.widgets.bottomsheet.R.layout.sheet_row_item, parent, false);
        view.setVisibility(4);
        return view;
    }

    private View getMenuView(final MenuItem item, ViewGroup parent) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(this.mContext).inflate(com.odoo.widgets.bottomsheet.R.layout.sheet_row_item, parent, false);
        ImageView menu_icon = (ImageView)view.findViewById(com.odoo.widgets.bottomsheet.R.id.menu_icon);
        TextView menu_title = (TextView)view.findViewById(com.odoo.widgets.bottomsheet.R.id.menu_title);
        menu_icon.setImageDrawable(item.getIcon());
        menu_title.setText(item.getTitle());
        menu_title.setTextColor(this.mBuilder.getTextColor());
        menu_icon.setColorFilter(this.mBuilder.getIconColor());
        if(this.mItemListener != null) {
            view.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    (new Handler()).postDelayed(new Runnable() {
                        public void run() {
                            BottomSheetNew.this.mItemListener.onItemClick(BottomSheetNew.this, item, BottomSheetNew.this.mBuilder.getExtraData());
                        }
                    }, 100L);
                }
            });
        }

        return view;
    }

    @SuppressLint("WrongConstant")
    private void prepareTitle(RelativeLayout layout) {
        String title = this.mBuilder.getSheetTitle();
        final BottomSheetListenersNew.OnSheetActionClickListener actionListener = this.mBuilder.getActionListener();
        TextView sheet_title = (TextView)layout.findViewById(R.id.sheet_title);
        ImageView sheet_action = (ImageView)layout.findViewById(R.id.sheet_action);
        if(actionListener != null) {
            sheet_action.setVisibility(0);
            sheet_action.setColorFilter(this.mBuilder.getIconColor());
            if(this.mBuilder.getActionIcon() != 0) {
                sheet_action.setImageResource(this.mBuilder.getActionIcon());
            } else {
                sheet_action.setImageResource(com.odoo.widgets.bottomsheet.R.drawable.ic_launcher);
            }

            sheet_action.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    actionListener.onSheetActionClick(BottomSheetNew.this, BottomSheetNew.this.mBuilder.getExtraData());
                }
            });
        } else {
            sheet_action.setVisibility(4);
        }

        if(title != null) {
            layout.findViewById(R.id.sheet_title_view).setVisibility(1);
            layout.findViewById(R.id.sheet_title_divider).setVisibility(1);
            layout.findViewById(R.id.sheet_title_divider).setBackgroundColor(Color.BLACK);
            sheet_title.setText(title);
            sheet_title.setTextColor(this.mBuilder.getTextColor());
        } else {
            layout.findViewById(R.id.sheet_title_view).setVisibility(8);
            layout.findViewById(R.id.sheet_title_divider).setVisibility(8);
            layout.findViewById(R.id.sheet_title_divider).setBackgroundColor(Color.BLACK);
        }

    }

    private void init(Context context) {
        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.sheet, this, true);
        LayoutParams params = new LayoutParams(this.getScreenWidth(), -2);
        layout.setLayoutParams(params);
        this.prepareTitle(layout);
        this.prepareMenus(layout);
        @SuppressLint("ResourceType") ViewGroup root = (ViewGroup)((Activity)context).findViewById(16908290);
        FrameLayout.LayoutParams frame_params = new FrameLayout.LayoutParams(this.getScreenWidth(), -2);
        frame_params.gravity = 81;
        FrameLayout overlay = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.sheet_overlay, root, false);
        root.addView(overlay);
        overlay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BottomSheetNew.this.dismiss();
            }
        });
        root.addView(this, frame_params);
        this.mShowing = Boolean.valueOf(true);
        Animation slideIn = AnimationUtils.loadAnimation(this.getContext(), com.odoo.widgets.bottomsheet.R.anim.sheet_in);
        slideIn.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        this.startAnimation(slideIn);
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager)this.mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        return orientation == 2?point.y:point.x;
    }

    public static class Builder {
        private Context mContext;
        private Resources mRes;
        private String mSheetTitle = null;
        private Integer mSheetMenu;
        private BottomSheetListenersNew.OnSheetItemClickListener mItemListener = null;
        private BottomSheetListenersNew.OnSheetActionClickListener mOnSheetActionClickListener = null;
        private BottomSheetListenersNew.OnSheetMenuCreateListener mOnSheetMenuCreateListener = null;
        private Integer textColor = Integer.valueOf(-16777216);
        private Integer iconColor = Integer.valueOf(-16777216);
        private Object data = null;
        private Integer actionIcon = Integer.valueOf(0);

        public Builder(Context context) {
            this.mContext = context;
            this.mRes = this.mContext.getResources();
        }

        public Builder title(int res_id) {
            try {
                this.title(this.mRes.getString(res_id));
            } catch (Exception var3) {
                Log.e(BottomSheetNew.TAG, var3.getMessage());
            }

            return this;
        }

        public Builder setData(Object extra_data) {
            this.data = extra_data;
            return this;
        }

        public Builder setActionIcon(int res_id) {
            this.actionIcon = Integer.valueOf(res_id);
            return this;
        }

        public int getActionIcon() {
            return this.actionIcon.intValue();
        }

        public Object getExtraData() {
            return this.data;
        }

        public Builder title(CharSequence title) {
            this.mSheetTitle = title.toString();
            return this;
        }

        public Builder actionListener(BottomSheetListenersNew.OnSheetActionClickListener listener) {
            this.mOnSheetActionClickListener = listener;
            return this;
        }

        public BottomSheetListenersNew.OnSheetActionClickListener getActionListener() {
            return this.mOnSheetActionClickListener;
        }

        public Builder setIconColor(int color) {
            this.iconColor = Integer.valueOf(color);
            return this;
        }

        public Builder setTextColor(int color) {
            this.textColor = Integer.valueOf(color);
            return this;
        }

        public int getTextColor() {
            return this.textColor.intValue();
        }

        public int getIconColor() {
            return this.iconColor.intValue();
        }

        public Builder menu(int menu_res_id) {
            this.mSheetMenu = Integer.valueOf(menu_res_id);
            return this;
        }

        public int getSheetMenu() {
            return this.mSheetMenu.intValue();
        }

        public Builder listener(BottomSheetListenersNew.OnSheetItemClickListener listener) {
            this.mItemListener = listener;
            return this;
        }

        public BottomSheetListenersNew.OnSheetItemClickListener getSheetItemListener() {
            return this.mItemListener;
        }

        public String getSheetTitle() {
            return this.mSheetTitle;
        }

        public BottomSheetNew create() {
            return (new BottomSheetNew(this.mContext)).setBuilder(this);
        }

        public Builder setOnSheetMenuCreateListener(BottomSheetListenersNew.OnSheetMenuCreateListener listener) {
            this.mOnSheetMenuCreateListener = listener;
            return this;
        }

        public BottomSheetListenersNew.OnSheetMenuCreateListener getOnSheetMenuCreateListener() {
            return this.mOnSheetMenuCreateListener;
        }
    }
}