package de.metaphoriker.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;

public class FuckYouControl {

  private final List<Region> overlays = new ArrayList<>();
  private final Map<Region, Boolean> layoutsNeeded = new WeakHashMap<>();

  public FuckYouControl(Node root) {

    EventDispatcher eventDispatcher = root.getEventDispatcher();
    root.setEventDispatcher(
        (event, tail) -> {
          Event dispatched = eventDispatcher.dispatchEvent(event, tail);

          List<Node> visible = new ArrayList<>(overlays.size());

          for (Node node : overlays) if (node.isVisible()) visible.add(node);

          if (!visible.isEmpty() && event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {

            PickResult pickResult = ((MouseEvent) event).getPickResult();

            Node intersectedNode = pickResult.getIntersectedNode();

            for (Node node : visible) if (isOutside(intersectedNode, node)) node.setVisible(false);
          }

          return dispatched;
        });
  }

  public static void show(Region overlay, Node target) {

    overlay.setVisible(true);
    overlay.requestLayout();

    RelocateBinding binding = new RelocateBinding(overlay, overlay.getParent(), target);

    overlay.layoutXProperty().bind(binding.asX());
    overlay.layoutYProperty().bind(binding.asY());

    overlay
        .visibleProperty()
        .addListener(
            new ChangeListener() {

              @Override
              public void changed(ObservableValue observable, Object oldValue, Object newValue) {

                overlay.layoutXProperty().unbind();
                overlay.layoutYProperty().unbind();

                overlay.visibleProperty().removeListener(this);
              }
            });
  }

  public static void hide(Region overlay) {
    overlay.setVisible(false);
  }

  private static boolean isOutside(Node node, Node target) {

    if (target == node) return false;

    Parent parent = node.getParent();

    while (parent != null && parent != target) parent = parent.getParent();

    return target != parent;
  }

  public void addOverlay(Region node) {

    overlays.add(node);

    node.setManaged(false);
    node.needsLayoutProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!Boolean.TRUE.equals(newValue)) return;

              layoutsNeeded.put(node, true);
            });
  }

  private static final class RelocateBinding extends ObjectBinding<Point2D> {

    private final Region overlay;
    private final Parent parent;
    private final Node target;

    private RelocateBinding(Region overlay, Parent parent, Node target) {

      this.overlay = overlay;
      this.parent = parent;
      this.target = target;

      super.bind(overlay.widthProperty());
      super.bind(overlay.heightProperty());
      super.bind(overlay.needsLayoutProperty());
      super.bind(parent.layoutBoundsProperty());
      super.bind(target.layoutBoundsProperty());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObservableList<?> getDependencies() {
      return FXCollections.observableArrayList(
          overlay.widthProperty(),
          overlay.heightProperty(),
          parent.layoutBoundsProperty(),
          target.layoutBoundsProperty());
    }

    @Override
    protected Point2D computeValue() {
      Point2D targetPosition =
          target.localToScene(
              target.getLayoutBounds().getMinX(), target.getLayoutBounds().getMinY());
      return targetPosition.add(-150, -150); // TODO: hardcoded offset
    }

    private DoubleExpression asX() {
      return Bindings.createDoubleBinding(() -> get().getX(), this);
    }

    private DoubleExpression asY() {
      return Bindings.createDoubleBinding(() -> get().getY(), this);
    }
  }
}
