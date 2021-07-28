package org.example.dao;

import org.apache.ibatis.annotations.Param;
import org.example.dataobject.ItemStockDO;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemStockDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    int insert(ItemStockDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    int insertSelective(ItemStockDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    ItemStockDO selectByPrimaryKey(Integer id);

    ItemStockDO selectByItemId(Integer itemId);

    int decreaseStock(@Param("itemid")Integer itemid, @Param("amount")Integer amount);


    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    int updateByPrimaryKeySelective(ItemStockDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_info
     *
     * @mbg.generated Wed Jun 16 15:24:11 CST 2021
     */
    int updateByPrimaryKey(ItemStockDO record);
}