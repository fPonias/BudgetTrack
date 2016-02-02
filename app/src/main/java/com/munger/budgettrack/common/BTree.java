package com.munger.budgettrack.common;

import java.util.ArrayList;

/**
 * Created by codymunger on 1/31/16.
 */
public class BTree<T extends Comparable>
{
    public T item;
    public ArrayList<T> items;
    public BTree<T> left;
    public BTree<T> right;
    public BTree<T> parent;

    public BTree(BTree<T> parent)
    {
        item = null;
        items = new ArrayList<>();
        left = null;
        right = null;
        this.parent = parent;

    }

    public void add(T item)
    {
        if (this.item == null)
        {
            this.item = item;
            items.add(item);
            return;
        }

        int value = this.item.compareTo(item);
        if (value == 0)
        {
            if (items.contains(item))
                return;

            items.add(item);
            return;
        }
        else if (value < 0)
        {
            if (left == null)
                left = new BTree<T>(this);

            left.add(item);
        }
        else if (value > 0)
        {
            if (right == null)
                right = new BTree<T>(this);

            right.add(item);
        }
    }

    public BTree<T> find(T item)
    {
        return find(item, false);
    }

    public BTree<T> find(T item, boolean closest)
    {
        if (this.item == null)
        {
            if (closest)
                return this;
            else
                return null;
        }

        int value = this.item.compareTo(item);

        if (value == 0)
            return this;
        else if (value < 0)
        {
            if (left != null)
                return left.find(item, closest);
            else if (closest)
                return this;
        }
        else
        {
            if (right != null)
                return right.find(item, closest);
            else if (closest)
                return this;
        }

        return null;
    }

    public void delete(T item)
    {
        BTree<T> node = this.find(item, false);

        if (node != null)
            node.items.remove(item);

        if (item == node.item && node.items.size() > 0)
            node.item = node.items.get(0);

        if (node.items.size() == 0)
        {
            if (node.left == null && node.right == null)
            {
                if (node.parent == null)
                    return;

                if (node.parent.left == node)
                    node.parent.left = null;
                else if (node.parent.right == node)
                    node.parent.right = null;

                return;
            }

            if (node.left != null && node.right == null)
            {
                if (node.parent == null)
                    return;

                if (node.parent.left == node)
                    node.parent.left = node.left;
                else
                    node.parent.right = node.left;
            }
            else if(node.right != null && node.left == null)
            {
                if (node.parent == null)
                    return;

                if (node.parent.left == node)
                    node.parent.left = node.right;
                else
                    node.parent.right = node.right;
            }
            else
            {
                BTree<T> min = node.right;
                while (min.left != null)
                    min = min.left;

                node.item = min.item;
                node.items = min.items;

                if (node.parent == null)
                    return;

                if (min.parent.left == min)
                    min.parent.left = null;
                else
                    min.parent.right = null;
            }
        }

    }

    public ArrayList<T> getOrderedList(T target, boolean lessThan)
    {
        ArrayList<T> ret = new ArrayList<>();

        BTree<T> closest = this.find(target, true);

        if (closest != null)
        {
            if (lessThan && closest.left != null)
                closest.left.appendChildren(ret);

            if (closest.item != null)
            {
                int value = closest.item.compareTo(target);

                if (value == 0)
                {
                    for(T i : closest.items)
                        ret.add(i);
                }
            }

            if (!lessThan && closest.right != null)
                closest.right.appendChildren(ret);
        }

        return ret;
    }

    private void appendChildren(ArrayList<T> ret)
    {
        if (left != null)
            left.appendChildren(ret);

        for(T i : items)
            ret.add(i);

        if (right != null)
            right.appendChildren(ret);
    }
}
