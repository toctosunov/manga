package kgz.senior.manga;

public class ButtonConfig {
    private String text;
    private String url;
    private String textColor;
    private String backgroundColor;
    private boolean useGlobalColors;

    public ButtonConfig(String text, String url) {
        this.text = text;
        this.url = url;
        this.useGlobalColors = true;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isUseGlobalColors() {
        return useGlobalColors;
    }

    public void setUseGlobalColors(boolean useGlobalColors) {
        this.useGlobalColors = useGlobalColors;
    }
} 