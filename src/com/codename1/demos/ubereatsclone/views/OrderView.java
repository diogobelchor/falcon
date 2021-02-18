/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package com.codename1.demos.ubereatsclone.views;

import com.codename1.components.MultiButton;
import com.codename1.demos.ubereatsclone.Util;
import com.codename1.demos.ubereatsclone.interfaces.Address;
import com.codename1.demos.ubereatsclone.interfaces.PaymentMethod;
import com.codename1.demos.ubereatsclone.interfaces.Restaurant;
import com.codename1.demos.ubereatsclone.models.AccountModel;
import com.codename1.demos.ubereatsclone.models.CompletedOrderModel;
import com.codename1.demos.ubereatsclone.models.PaymentMethodModel;
import com.codename1.demos.ubereatsclone.models.RestaurantModel;
import com.codename1.l10n.L10NManager;
import com.codename1.rad.controllers.ActionSupport;
import com.codename1.rad.controllers.FormController;
import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityList;
import com.codename1.rad.models.Property;
import com.codename1.rad.nodes.ActionNode;
import com.codename1.rad.nodes.Node;
import com.codename1.rad.ui.AbstractEntityView;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.codename1.ui.CN.convertToPixels;
import static com.codename1.ui.util.Resources.getGlobalResources;

public class OrderView extends AbstractEntityView {

    Node viewNode;
    Property orderProp;
    List dishes = new ArrayList();
    Label totalPriceLabel, itemTotalCostLabel;
    Container totalPriceCnt, itemTotalCostCnt;
    AccountModel account;
    MultiButton deliverToButton;
    PaymentMethodModel paymentMethod;
    PaymentMethodView paymentView;

    public static final ActionNode.Category COMPLETE_ORDER = new ActionNode.Category();
    public static final ActionNode.Category CHANGE_DELIVERY_ADDRESS = new ActionNode.Category();
    public static final ActionNode.Category CHANGE_DELIVERY_PAYMENT = new ActionNode.Category();

    public OrderView(Entity restEntity, Entity profileEntity, Node viewNode, Node mainWindowNode) {
        super(restEntity);
        account = (AccountModel) profileEntity;
        setLayout(BoxLayout.y());
        setUIID("OrderCnt");
        setScrollableY(true);

        this.viewNode = viewNode;
        orderProp = restEntity.findProperty(Restaurant.order);

        Button backButton = new Button(FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
        backButton.setUIID("AddDishBackButton");
        backButton.addActionListener(evt -> {
            evt.consume();
            ActionSupport.dispatchEvent(new FormController.FormBackEvent(backButton));
        });
        Label headerLabel = new Label("MY ORDERS", "AddDishHeaderLabel");
        Container headerCnt = BorderLayout.center(headerLabel).add(BorderLayout.WEST, backButton);
        headerCnt.setUIID("AddDishHeaderCnt");
        add(headerCnt);

        if (restEntity.get(orderProp) instanceof EntityList) {
            EntityList<Entity> dishList = (EntityList) (restEntity.get(Restaurant.order));
            for(Entity dishOrderEntity : dishList){
                DishOrderPreview dish = new DishOrderPreview(dishOrderEntity, viewNode);
                dishes.add(dish);
                add(dish);
            }
        }
        MultiButton addPromoCodeButton = new MultiButton("ADD PROMO CODE");
        addPromoCodeButton.setUIID("AddPromoCodeButton");
        addPromoCodeButton.setUIIDLine1("AddPromoCodeButtonText");
        Image promCodeImage = getGlobalResources().getImage("ticket-icon.png").scaled(convertToPixels(4), convertToPixels(4));
        addPromoCodeButton.setIcon(promCodeImage);
        Image gotoIcon = FontImage.createMaterial(FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, UIManager.getInstance().getComponentStyle("GoToIcon"));
        addPromoCodeButton.setEmblem(gotoIcon);
        addPromoCodeButton.setEmblemPosition("East");
        add(addPromoCodeButton);

        EntityList<Entity> cards = (EntityList<Entity>) account.getCreditCards();
        if (cards.size() == 0){
            paymentMethod = new PaymentMethodModel(PaymentMethod.CASH, null);
        }else{
            paymentMethod = new PaymentMethodModel(PaymentMethod.CREDIT_CARD, cards.get(0));
        }

        paymentView = new PaymentMethodView(paymentMethod, viewNode);
        add(paymentView);

        deliverToButton = new MultiButton("DELIVER TO");
        deliverToButton.setUIID("ManageAddressButton");
        deliverToButton.setUIIDLine1("ManageAddressButtonLine1");
        deliverToButton.setUIIDLine2("ManageAddressButtonLine2");
        Image mapPinIcon = getGlobalResources().getImage("map-pin-icon.png").scaled(convertToPixels(4), convertToPixels(4));
        deliverToButton.setIcon(mapPinIcon);
        deliverToButton.setEmblem(gotoIcon);
        deliverToButton.setTextLine2("");
        Entity address = ((AccountModel)profileEntity).getDefaultAddress();
        if (address != null){
            deliverToButton.setTextLine2(address.getText(Address.city) + ", " + address.getText(Address.street));
        }
        deliverToButton.addActionListener(evt->{
            evt.consume();
            ActionNode action = viewNode.getInheritedAction(CHANGE_DELIVERY_ADDRESS);
            if (action != null) {
                action.fireEvent(null, OrderView.this);
            }
        });
        add(deliverToButton);

        Label deliveryFeeHeaderLabel = new Label("Delivery Fee", "OrderDeliveryFeeHeader");
        Label deliveryFeeLabel = new Label(restEntity.getDouble(Restaurant.deliveryFee) + " " + L10NManager.getInstance().getCurrencySymbol(), "OrderDeliveryFee");
        Container deliveryFeeCnt = BorderLayout.centerCenterEastWest(null, deliveryFeeLabel, deliveryFeeHeaderLabel);
        deliveryFeeCnt.setUIID("DeliveryFeeCnt");

        Label itemTotalHeaderLabel = new Label("Item Total", "ItemTotalHeader");
        itemTotalCostLabel = new Label("", "ItemTotalCost");
        itemTotalCostCnt = BorderLayout.centerCenterEastWest(null, itemTotalCostLabel, itemTotalHeaderLabel);
        itemTotalCostCnt.setUIID("v");

        Label totalHeaderLabel = new Label("Total:", "TotalCostHeader");
        totalPriceLabel = new Label("", "TotalCost");
        totalPriceCnt = BorderLayout.centerCenterEastWest(null, totalPriceLabel, totalHeaderLabel);
        totalPriceCnt.setUIID("TotalCostCnt");

        Button confirmOrder = new Button("Confirm Order", "OrderConfirmButton");
        confirmOrder.addActionListener(evt->{
            evt.consume();
            ActionNode action = mainWindowNode.getInheritedAction(COMPLETE_ORDER);
            if (action != null) {
                int year = Calendar.getInstance().get(Calendar.YEAR);
                int month = Calendar.getInstance().get(Calendar.MONTH);
                int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                String date = month + "." + day + "." + year;
                action.fireEvent(new CompletedOrderModel(restEntity, (EntityList)restEntity.get(Restaurant.order), date, Address.HOME), OrderView.this);
            }
        });

        Container summaryCnt = new Container(new BorderLayout(), "OrderSummaryCnt");
        summaryCnt.add(BorderLayout.NORTH, BoxLayout.encloseY(deliveryFeeCnt, itemTotalCostCnt));
        summaryCnt.add(BorderLayout.CENTER, totalPriceCnt);

        add(summaryCnt);
        add(confirmOrder);
        update();
    }

    @Override
    public void update() {
        itemTotalCostLabel.setText(Util.getPriceAsString(((RestaurantModel)getEntity()).getTotalItemPrice()));
        totalPriceLabel.setText(Util.getPriceAsString(((RestaurantModel)getEntity()).getTotalPrice()));
        itemTotalCostCnt.revalidateWithAnimationSafety();
        Entity address = account.getDefaultAddress();
        if (address != null){
            deliverToButton.setTextLine2(address.getText(Address.city) + ", " + address.getText(Address.street));
        }
        deliverToButton.revalidateWithAnimationSafety();
        paymentView.update();
        revalidateWithAnimationSafety();
    }

    @Override
    public void commit() {

    }

    @Override
    public Node getViewNode() {
        return viewNode;
    }

    public void setCreditCard(Entity card){
        paymentMethod.set(PaymentMethod.method, PaymentMethod.CREDIT_CARD);
        paymentMethod.set(PaymentMethod.creditCard, card);
        paymentView.update();
    }

}
