package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.focus.ContainerFocusListener;
import lsfusion.client.base.focus.FormFocusTraversalPolicy;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.event.KeyInputEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClientFormLayout extends JPanel {

    public Dimension getMaxPreferredSize() {
        return AbstractClientContainerView.getMaxPreferredSize(mainContainer,containerViews, false); // в BOX container'е берем явный size (предполагая что он используется не как базовый размер с flex > 0, а конечный)
    }

    private final ClientFormController form;
    private final ClientContainer mainContainer;

    private FormFocusTraversalPolicy policy;
    
    private Map<ClientContainer, ClientContainerView> containerViews = new HashMap<>();
    
    private boolean blocked;

    @SuppressWarnings({"FieldCanBeLocal"})
    private FocusListener focusListener;

    public ClientFormLayout(ClientFormController iform, ClientContainer imainContainer) {
        this.form = iform;
        this.mainContainer = imainContainer;

        policy = new FormFocusTraversalPolicy();

        setFocusCycleRoot(true);
        setFocusTraversalPolicy(policy);

        // создаем все контейнеры на форме
        createContainerViews(mainContainer);

        setLayout(new BorderLayout());
        add(containerViews.get(mainContainer).getView(), BorderLayout.CENTER);

        //todo: think about scrollpane, when window size is too small
//        JScrollPane scroll = new JScrollPane(mainContainer);
//        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//        add(scroll, BorderLayout.CENTER);

        // приходится делать StrongRef, иначе он тут же соберется сборщиком мусора так как ContainerFocusListener держит его как WeakReference
        focusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                form.gainedFocus();
                MainFrame.instance.setCurrentForm(form);
            }
        };

        ContainerFocusListener.addListener(this, focusListener);

        // вот таким вот маразматичным способом делается, чтобы при нажатии мышкой в ClientFormController фокус оставался на ней, а не уходил куда-то еще
        // теоретически можно найти способ как это сделать не так извращенно, но копаться в исходниках Swing'а очень долго
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    public ClientContainerView getContainerView(ClientContainer container) {
        return containerViews.get(container);
    }

    // метод рекурсивно создает для каждого ClientContainer соответствующий ContainerView
    private void createContainerViews(ClientContainer container) {
        ClientContainerView containerView;
        if (container.isLinear()) {
            containerView = new LinearClientContainerView(this, container);
        } else if (container.isSplit()) {
            containerView = new SplitClientContainerView(this, container);
        } else if (container.isTabbed()) {
            containerView = new TabbedClientContainerView(this, container, form);
        } else if (container.isColumns()) {
            containerView = new ColumnsClientContainerView(this, container);
        } else if (container.isScroll()) {
            containerView = new ScrollClientContainerView(this, container);
        } else if (container.isFlow()) {
            throw new IllegalStateException("Flow isn't implemented yet");
        } else {
            throw new IllegalStateException("Illegal container type");
        }

        containerViews.put(container, containerView);

        if (container.container != null) {
            add(container, containerView.getView());
        }

        for (ClientComponent child : container.children) {
            if (child instanceof ClientContainer) {
                createContainerViews((ClientContainer) child);
            }
        }
    }

    // вообще раньше была в validate, calculatePreferredSize видимо для устранения каких-то визуальных эффектов
    // но для activeTab нужно вызвать предварительно, так как вкладка может только-только появится
    // пока убрал (чтобы было как в вебе), но если будут какие-то нежелательные эффекты, можно будет вернуть а в activeElements поставить только по условию, что есть activeTabs или activeProps
    public void preValidateMainContainer() { // hideEmptyContainerViews 
        autoShowHideContainers(mainContainer);
    }

    private void autoShowHideContainers(ClientContainer container) {
        ClientContainerView containerView = containerViews.get(container);
//        if (!containerView.getView().isValid()) { // непонятная проверка, valid достаточно непредсказуемая штука и логически не сильно связано с логикой visibility container'ов + вызывается огранич
            int childCnt = containerView.getChildrenCount();
            boolean hasVisible = false;
            for (int i = 0; i < childCnt; ++i) {
                ClientComponent child = containerView.getChild(i);
                Component childView = containerView.getChildView(i);
                if (child instanceof ClientContainer) {
                    autoShowHideContainers((ClientContainer) child);
                }

                if (childView.isVisible()) {
                    hasVisible = true;
                }
            }
            containerView.getView().setVisible(hasVisible);
            containerView.updateLayout();
//        }
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponent key, JComponentPanel view) {
        if (key != null) {
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && !containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.add(key, view);

                if (key.defaultComponent) {
                    policy.addDefault(view);
                }
                return true;
            }
        }
        return false;
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponent key, Component view) {
        if (key != null) {
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.remove(key);
                if (key.defaultComponent) {
                    policy.removeDefault(view);
                }
                return true;
            }
        }
        return false;
    }

    public void addBinding(KeyStroke key, String id, AbstractAction action) {
        Object oldId = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(key);

        String resultId = id;
        Action resultAction = new ClientActionProxy(form, action);
        if (oldId != null) {
            Action oldAction = getActionMap().get(oldId);
            if (oldAction != null) {
                MultiAction multiAction = new MultiAction(oldAction);
                multiAction.addAction(resultAction);
                resultId += " and " + oldId;
                resultAction = multiAction;
            }
        }

        getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, resultId);
        getActionMap().put(resultId, resultAction);
    }

    @Override
    public Dimension getMinimumSize() {
        //для таблиц с большим числом колонок возвращается огромное число и тогда Docking Frames пытается всё отдать под форму
        return new Dimension(0, 0);
    }

    public ClientGroupObject getGroupObject(Component comp) {
        while (comp != null && !(comp instanceof Window) && comp != this) {
            if (comp instanceof JComponent) {
                ClientGroupObject groupObject = (ClientGroupObject) ((JComponent) comp).getClientProperty("groupObject");
                if (groupObject != null)
                    return groupObject;
            }
            comp = comp.getParent();
        }
        return null;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            if(form.processBinding(new KeyInputEvent(ks), () -> getGroupObject(ke.getComponent())))
                return true;

        return super.processKeyBinding(ks, ke, condition, pressed);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
